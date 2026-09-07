package com.n3d.netlab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.i18n.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class ThemeMode { System, Light, Dark }

/**
 * How the app is set up on **this** device.
 *
 * Deliberately not part of [Progress] and so not synced: which language and
 * theme suit a phone is a fact about the phone, not about the learner, and the
 * web version keeps its settings local for the same reason.
 */
data class Settings(
    val lang: Lang,
    val theme: ThemeMode = ThemeMode.System,
    val kind: ExerciseKind = ExerciseKind.Vlsm,
    val difficulty: Difficulty = Difficulty.Easy,
)

/**
 * Everything read back at startup, in one shot.
 *
 * [guest] is what was earned before anybody signed in and survives signing out;
 * [cached] is the last known copy of [account]'s progress, kept so the app opens
 * on the right numbers with no signal rather than on zeroes.
 */
data class Stored(
    val settings: Settings,
    val token: String?,
    val account: Account?,
    val guest: Progress,
    val cached: Progress,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netlab")

class Prefs(private val context: Context) {

    /**
     * A Czech phone opens the app in Czech.
     *
     * The setting still wins once it has been touched — this only fills in the
     * first launch, where guessing from the system locale is a better default
     * than making a Czech user find the language switch in English first.
     */
    private fun deviceLang(): Lang =
        if (Locale.getDefault().language.equals("cs", ignoreCase = true)) Lang.Cs else Lang.En

    val flow: Flow<Stored> = context.dataStore.data.map { p ->
        val id = p[KeyUserId]
        val token = p[KeyToken]
        Stored(
            settings = Settings(
                lang = p[KeyLang]?.let { name -> Lang.entries.firstOrNull { it.name == name } } ?: deviceLang(),
                theme = p[KeyTheme]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.System,
                kind = p[KeyKind]?.let { name -> ExerciseKind.entries.firstOrNull { it.name == name } } ?: ExerciseKind.Vlsm,
                difficulty = p[KeyDifficulty]?.let { name -> Difficulty.entries.firstOrNull { it.name == name } } ?: Difficulty.Easy,
            ),
            token = token,
            account = if (id == null) {
                null
            } else {
                Account(id, p[KeyUserEmail].orEmpty(), p[KeyUserName].orEmpty(), p[KeyUserAdmin] == true)
            },
            guest = guestProgress(p),
            // Belongs to whoever it was written for. A different account signing
            // in on the same phone starts from the server, not from this.
            cached = if (id != null && p[KeyCachedFor] == id) ProgressJson.decode(p[KeyCached]) else Progress(),
        )
    }

    /**
     * The guest copy, migrating the loose counters version 1.0 wrote.
     *
     * That version had no accounts and no blob — just `solved`, `streak` and
     * `best` sitting in their own keys. Somebody who has been using the app
     * should not come back from an update to a reset streak, so the old keys are
     * read until the first write replaces them.
     */
    private fun guestProgress(p: Preferences): Progress {
        p[KeyGuest]?.let { return ProgressJson.decode(it) }
        return Progress(
            chapters = p[KeyLegacyChapters].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            solved = p[KeyLegacySolved] ?: 0,
            clean = p[KeyLegacyClean] ?: 0,
            streak = p[KeyLegacyStreak] ?: 0,
            best = p[KeyLegacyBest] ?: 0,
            vlsm = p[KeyLegacyVlsm] ?: 0,
            analyze = p[KeyLegacyAnalyze] ?: 0,
            // Zero, not "now": an unsynced local copy must never look newer than
            // the account's own, or the first sign-in would roll the account back.
            updatedAt = 0,
        )
    }

    suspend fun setLang(value: Lang) = context.dataStore.edit { it[KeyLang] = value.name }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[KeyTheme] = value.name }
    suspend fun setKind(value: ExerciseKind) = context.dataStore.edit { it[KeyKind] = value.name }
    suspend fun setDifficulty(value: Difficulty) = context.dataStore.edit { it[KeyDifficulty] = value.name }

    // ---- progress ------------------------------------------------------------

    suspend fun writeProgress(progress: Progress, forAccount: Int?) =
        context.dataStore.edit { p ->
            if (forAccount == null) {
                p[KeyGuest] = ProgressJson.encode(progress)
            } else {
                p[KeyCached] = ProgressJson.encode(progress)
                p[KeyCachedFor] = forAccount
            }
        }

    /** Once the guest copy has been folded into an account it must go, or the
     *  next sign-in adds the same exercises again. */
    suspend fun clearGuestProgress() = context.dataStore.edit { p ->
        p.remove(KeyGuest)
        p.remove(KeyLegacyChapters)
        p.remove(KeyLegacySolved)
        p.remove(KeyLegacyClean)
        p.remove(KeyLegacyStreak)
        p.remove(KeyLegacyBest)
        p.remove(KeyLegacyVlsm)
        p.remove(KeyLegacyAnalyze)
    }

    // ---- session -------------------------------------------------------------

    suspend fun saveSession(token: String?, account: Account, progress: Progress) =
        context.dataStore.edit { p ->
            // A refresh that carries no new cookie keeps the one already stored;
            // only signing out clears it.
            if (token != null) p[KeyToken] = token
            p[KeyUserId] = account.id
            p[KeyUserEmail] = account.email
            p[KeyUserName] = account.name
            p[KeyUserAdmin] = account.isAdmin
            p[KeyCached] = ProgressJson.encode(progress)
            p[KeyCachedFor] = account.id
        }

    suspend fun clearSession() = context.dataStore.edit { p ->
        p.remove(KeyToken)
        p.remove(KeyUserId)
        p.remove(KeyUserEmail)
        p.remove(KeyUserName)
        p.remove(KeyUserAdmin)
        p.remove(KeyCached)
        p.remove(KeyCachedFor)
    }

    private companion object {
        val KeyLang = stringPreferencesKey("lang")
        val KeyTheme = stringPreferencesKey("theme")
        val KeyKind = stringPreferencesKey("kind")
        val KeyDifficulty = stringPreferencesKey("difficulty")

        val KeyGuest = stringPreferencesKey("progressGuest")
        val KeyCached = stringPreferencesKey("progressAccount")
        val KeyCachedFor = intPreferencesKey("progressAccountId")

        val KeyToken = stringPreferencesKey("sessionToken")
        val KeyUserId = intPreferencesKey("userId")
        val KeyUserEmail = stringPreferencesKey("userEmail")
        val KeyUserName = stringPreferencesKey("userName")
        val KeyUserAdmin = booleanPreferencesKey("userAdmin")

        // Written by 1.0, read until the first save of the new blob.
        val KeyLegacySolved = intPreferencesKey("solved")
        val KeyLegacyStreak = intPreferencesKey("streak")
        val KeyLegacyBest = intPreferencesKey("best")
        val KeyLegacyClean = intPreferencesKey("clean")
        val KeyLegacyVlsm = intPreferencesKey("solvedVlsm")
        val KeyLegacyAnalyze = intPreferencesKey("solvedAnalyze")
        val KeyLegacyChapters = stringSetPreferencesKey("chaptersRead")
    }
}
