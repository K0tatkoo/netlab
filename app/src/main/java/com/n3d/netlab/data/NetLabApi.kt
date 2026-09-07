package com.n3d.netlab.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Who is signed in. The server hands back exactly these four fields. */
data class Account(
    val id: Int,
    val email: String,
    val name: String,
    val isAdmin: Boolean,
) {
    /** What to show: the display name if there is one, the address otherwise. */
    val label: String get() = name.ifBlank { email }
}

/** Half a sign-in: the password was accepted, the code has been emailed. */
data class Challenge(val token: String, val email: String)

/** A whole one: a session cookie, the account, and its progress. */
data class Session(val token: String?, val account: Account, val progress: Progress)

/**
 * A failure with a name the UI can translate.
 *
 * The codes are the server's own (`credentials`, `taken`, `rate`, `expired`,
 * `attempts`, `mail`, `csrf`, `auth`) plus `network` for "never reached it" and
 * `server` for anything unrecognised. A dead network and a rejected request
 * want different words in front of somebody, so they are never conflated.
 */
class ApiException(val code: String) : Exception(code)

/**
 * The only four things this app asks a server for: who am I, sign in, sign out,
 * and here is my progress.
 *
 * Everything else — the course, the exercises, the grading, the walkthrough —
 * is computed on the device, so the app keeps working with no signal at all and
 * a failed request never costs anybody an answer they typed.
 *
 * Written against `java.net` rather than a client library on purpose: seven
 * endpoints and three JSON shapes do not justify a dependency, and the payloads
 * here are small enough to build by hand.
 */
class NetLabApi(
    private val baseUrl: String = BASE_URL,
    private val token: () -> String?,
) {

    suspend fun me(): Session? {
        val payload = call("/api/me")
        if (payload.isNull("user")) return null
        return session(payload, null)
    }

    suspend fun register(email: String, password: String, name: String, lang: String): Challenge =
        challenge(
            call(
                "/api/register",
                "POST",
                JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("name", name)
                    .put("lang", lang),
            ),
            email,
        )

    suspend fun login(email: String, password: String, lang: String): Challenge =
        challenge(
            call(
                "/api/login",
                "POST",
                JSONObject().put("email", email).put("password", password).put("lang", lang),
            ),
            email,
        )

    /** The second factor. Nothing before this point creates a session. */
    suspend fun verify(challenge: String, code: String): Session {
        var cookie: String? = null
        val payload = call(
            "/api/verify",
            "POST",
            JSONObject().put("challenge", challenge).put("code", code),
        ) { connection -> cookie = sessionCookie(connection) }
        return session(payload, cookie)
    }

    suspend fun resend(challenge: String, email: String, lang: String): Challenge =
        challenge(
            call(
                "/api/verify/resend",
                "POST",
                JSONObject().put("challenge", challenge).put("lang", lang),
            ),
            email,
        )

    /**
     * Asking for a reset link.
     *
     * The server answers ok whatever happens — unknown address, rate limit, even
     * a mail failure — so that the endpoint cannot be used to ask which
     * addresses have accounts. There is nothing here to branch on and this does
     * not try.
     */
    suspend fun requestReset(email: String, lang: String) {
        call("/api/reset/request", "POST", JSONObject().put("email", email).put("lang", lang))
    }

    suspend fun logout() {
        call("/api/logout", "POST", JSONObject())
    }

    suspend fun saveProgress(progress: Progress): Progress =
        ProgressJson.decode(
            call(
                "/api/progress",
                "PUT",
                JSONObject().put("progress", JSONObject(ProgressJson.encode(progress))),
            ).optJSONObject("progress") ?: throw ApiException("server"),
        )

    suspend fun mergeProgress(progress: Progress): Progress =
        ProgressJson.decode(
            call(
                "/api/progress/merge",
                "POST",
                JSONObject().put("progress", JSONObject(ProgressJson.encode(progress))),
            ).optJSONObject("progress") ?: throw ApiException("server"),
        )

    // ---- plumbing ------------------------------------------------------------

    private fun challenge(payload: JSONObject, fallbackEmail: String) = Challenge(
        token = payload.optString("challenge"),
        email = payload.optString("email").ifBlank { fallbackEmail },
    )

    private fun session(payload: JSONObject, cookie: String?) = Session(
        token = cookie,
        account = (payload.optJSONObject("user") ?: throw ApiException("server")).let {
            Account(
                id = it.optInt("id"),
                email = it.optString("email"),
                name = it.optString("name"),
                isAdmin = it.optBoolean("isAdmin"),
            )
        },
        progress = payload.optJSONObject("progress")?.let { ProgressJson.decode(it) } ?: Progress(),
    )

    private suspend fun call(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        onResponse: (HttpURLConnection) -> Unit = {},
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            // A custom header a cross-site form cannot set, which is what makes
            // the session cookie safe to accept on a POST. The server refuses
            // every mutating route without it.
            setRequestProperty("X-NetLab", "1")
            setRequestProperty("User-Agent", USER_AGENT)
            token()?.let { setRequestProperty("Cookie", "$COOKIE_NAME=$it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            onResponse(connection)
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                .orEmpty()
            val payload = runCatching { JSONObject(text) }.getOrNull()

            if (status !in 200..299) {
                throw ApiException(
                    payload?.optString("error")?.ifBlank { null }
                        ?: if (status == 429) "rate" else "server",
                )
            }
            // Every route here answers with a JSON object. Something else on a
            // 2xx means a proxy or an error page is talking, not the app, and
            // it must not reach the caller as a JSONException it cannot catch.
            payload ?: throw ApiException("server")
        } catch (e: IOException) {
            // Never reached the server, or lost it half way. Not the same thing
            // as being told no, and it does not want the same words.
            throw ApiException("network")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * The session cookie out of the response.
     *
     * The `__Host-` prefix is a rule browsers enforce about which cookies they
     * are willing to store; this client keeps the token itself, so all that is
     * needed is to read the value back out and send it again.
     */
    private fun sessionCookie(connection: HttpURLConnection): String? =
        connection.headerFields
            // Looked up case-insensitively rather than by "Set-Cookie": the map
            // is keyed by whatever the server actually sent, and HTTP/2 sends
            // every header name in lower case.
            .asSequence()
            .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value.orEmpty().asSequence() }
            .firstOrNull { it.startsWith("$COOKIE_NAME=") }
            ?.substringAfter('=')
            ?.substringBefore(';')
            ?.ifBlank { null }

    companion object {
        const val BASE_URL = "https://netlab.n3d-store.com"
        const val COOKIE_NAME = "__Host-netlab_session"
        private val USER_AGENT = "NetLab-Android/" + com.n3d.netlab.BuildConfig.VERSION_NAME
    }
}
