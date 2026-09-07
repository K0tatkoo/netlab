package com.n3d.netlab.data

import com.n3d.netlab.core.ExerciseKind
import org.json.JSONObject

/**
 * How far somebody has got, in the exact shape the web version stores.
 *
 * This is the only thing that travels between a phone and the server, so its
 * field names and its clamping rules have to match `src/progress.ts` and
 * `public/js/state.js` on the web character for character — a rename here is a
 * silent data loss there, and vice versa.
 */
data class Progress(
    /** Indices of the chapters that have been paged all the way through. */
    val chapters: Set<Int> = emptySet(),
    val solved: Int = 0,
    /** Finished with no stage needing a second go. */
    val clean: Int = 0,
    val streak: Int = 0,
    val best: Int = 0,
    val vlsm: Int = 0,
    val analyze: Int = 0,
    /** Milliseconds. Decides which of two copies of this account is newer. */
    val updatedAt: Long = 0,
) {
    val isEmpty: Boolean get() = chapters.isEmpty() && solved == 0 && best == 0

    fun withChapterRead(index: Int): Progress =
        if (index in chapters) this else copy(chapters = chapters + index)

    /** One finished exercise. Only a clean one extends the streak. */
    fun withResult(kind: ExerciseKind, clean: Boolean): Progress {
        val run = if (clean) streak + 1 else 0
        return copy(
            solved = solved + 1,
            clean = if (clean) this.clean + 1 else this.clean,
            streak = run,
            best = maxOf(best, run),
            vlsm = if (kind == ExerciseKind.Vlsm) vlsm + 1 else vlsm,
            analyze = if (kind == ExerciseKind.Analyze) analyze + 1 else analyze,
        )
    }

    /** Clears the score. Chapters read are reading, not score, and stay. */
    fun scoreCleared(): Progress =
        copy(solved = 0, clean = 0, streak = 0, best = 0, vlsm = 0, analyze = 0)
}

/**
 * The rules the server applies to anything it is handed, applied here too.
 *
 * Counters are clamped; `updatedAt` deliberately is **not** put through the
 * same clamp. It is a millisecond timestamp — about 1.8e12 — and a million
 * milliseconds is January 1970, so running it through the counter cap flattens
 * every stamp to the same number and silently breaks "is this copy newer than
 * that one". That happened once on the web and is the reason this comment is
 * here.
 */
object ProgressRules {
    const val MAX_COUNT = 1_000_000
    const val MAX_CHAPTERS = 100

    fun count(value: Long): Int = when {
        value <= 0 -> 0
        value > MAX_COUNT -> MAX_COUNT
        else -> value.toInt()
    }

    fun stamp(value: Long, now: Long): Long = when {
        value <= 0 -> 0
        value > now + 86_400_000L -> now + 86_400_000L
        else -> value
    }

    fun clean(progress: Progress, now: Long = System.currentTimeMillis()): Progress = Progress(
        chapters = progress.chapters.filter { it in 0 until MAX_CHAPTERS }.toSet(),
        solved = count(progress.solved.toLong()),
        clean = count(progress.clean.toLong()),
        streak = count(progress.streak.toLong()),
        best = count(progress.best.toLong()),
        vlsm = count(progress.vlsm.toLong()),
        analyze = count(progress.analyze.toLong()),
        updatedAt = stamp(progress.updatedAt, now),
    )

    /**
     * Union the chapters, add the counters, keep the better streak.
     *
     * Adding rather than taking the larger is right only because the guest copy
     * is deleted the moment a merge succeeds, so the same exercise can never be
     * counted twice.
     */
    fun merge(a: Progress, b: Progress, now: Long = System.currentTimeMillis()): Progress = clean(
        Progress(
            chapters = a.chapters + b.chapters,
            solved = a.solved + b.solved,
            clean = a.clean + b.clean,
            streak = maxOf(a.streak, b.streak),
            best = maxOf(a.best, b.best),
            vlsm = a.vlsm + b.vlsm,
            analyze = a.analyze + b.analyze,
            updatedAt = now,
        ),
        now,
    )
}

/**
 * The wire format.
 *
 * `chapters` is an object of index → timestamp on the web, because that is what
 * localStorage held first; only the key set is ever read, so this writes the
 * stamp it has and reads nothing but the keys.
 */
object ProgressJson {

    fun encode(progress: Progress): String {
        val chapters = JSONObject()
        progress.chapters.forEach { chapters.put(it.toString(), progress.updatedAt.coerceAtLeast(1)) }
        return JSONObject()
            .put("chapters", chapters)
            .put("solved", progress.solved)
            .put("clean", progress.clean)
            .put("streak", progress.streak)
            .put("best", progress.best)
            .put("byKind", JSONObject().put("vlsm", progress.vlsm).put("analyze", progress.analyze))
            .put("updatedAt", progress.updatedAt)
            .toString()
    }

    /** Anything at all may arrive here — a stale cache, a hostile server. */
    fun decode(raw: String?, now: Long = System.currentTimeMillis()): Progress {
        if (raw.isNullOrBlank()) return Progress()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return Progress()
        return decode(json, now)
    }

    fun decode(json: JSONObject, now: Long = System.currentTimeMillis()): Progress {
        val chapters = mutableSetOf<Int>()
        json.optJSONObject("chapters")?.let { map ->
            map.keys().forEach { key -> key.toIntOrNull()?.let(chapters::add) }
        }
        val byKind = json.optJSONObject("byKind")
        return ProgressRules.clean(
            Progress(
                chapters = chapters,
                solved = json.optLong("solved").toIntSafe(),
                clean = json.optLong("clean").toIntSafe(),
                streak = json.optLong("streak").toIntSafe(),
                best = json.optLong("best").toIntSafe(),
                vlsm = byKind?.optLong("vlsm")?.toIntSafe() ?: 0,
                analyze = byKind?.optLong("analyze")?.toIntSafe() ?: 0,
                updatedAt = json.optLong("updatedAt"),
            ),
            now,
        )
    }

    /** A count far outside Int is clamped by the rules anyway; this only stops
     *  the conversion itself from wrapping round into a negative. */
    private fun Long.toIntSafe(): Int = coerceIn(0L, ProgressRules.MAX_COUNT.toLong()).toInt()
}
