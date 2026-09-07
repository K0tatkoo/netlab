package com.n3d.netlab

import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.data.Progress
import com.n3d.netlab.data.ProgressJson
import com.n3d.netlab.data.ProgressRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The progress blob is the only thing this app and netlab.n3d-store.com have to
 * agree on, so these tests are really about the wire format and the two rules
 * that are easy to get subtly wrong: what counts as a clean solve, and the fact
 * that a timestamp is not a counter.
 */
class ProgressTest {

    private val now = 1_770_000_000_000L

    @Test
    fun `a clean solve extends the streak and a helped one ends it`() {
        var p = Progress()
        p = p.withResult(ExerciseKind.Vlsm, clean = true)
        p = p.withResult(ExerciseKind.Vlsm, clean = true)
        assertEquals(2, p.streak)
        assertEquals(2, p.best)
        assertEquals(2, p.clean)

        p = p.withResult(ExerciseKind.Analyze, clean = false)
        // Still solved — finishing counts — but the run is over and the best
        // stands.
        assertEquals(3, p.solved)
        assertEquals(2, p.clean)
        assertEquals(0, p.streak)
        assertEquals(2, p.best)
        assertEquals(2, p.vlsm)
        assertEquals(1, p.analyze)
    }

    @Test
    fun `resetting the score leaves the chapters read alone`() {
        val p = Progress(chapters = setOf(0, 1, 2), solved = 9, best = 4).scoreCleared()
        assertEquals(setOf(0, 1, 2), p.chapters)
        assertEquals(0, p.solved)
        assertEquals(0, p.best)
    }

    @Test
    fun `a chapter is only ever counted once`() {
        val p = Progress().withChapterRead(3).withChapterRead(3)
        assertEquals(setOf(3), p.chapters)
    }

    @Test
    fun `merging unions the chapters and adds the counters`() {
        val guest = Progress(chapters = setOf(0, 1), solved = 2, clean = 1, streak = 2, best = 2, vlsm = 2)
        val account = Progress(chapters = setOf(1, 5), solved = 7, clean = 5, streak = 1, best = 6, analyze = 7)
        val merged = ProgressRules.merge(account, guest, now)

        assertEquals(setOf(0, 1, 5), merged.chapters)
        assertEquals(9, merged.solved)
        assertEquals(6, merged.clean)
        // The better streak survives, and so does the better best.
        assertEquals(2, merged.streak)
        assertEquals(6, merged.best)
        assertEquals(2, merged.vlsm)
        assertEquals(7, merged.analyze)
        assertEquals(now, merged.updatedAt)
    }

    /**
     * The bug this file exists for. `updatedAt` is about 1.8e12 and the counter
     * cap is 1e6, so putting the stamp through the counter rule turns every save
     * into January 1970 — and "is this copy newer than that one" then compares
     * two equal numbers forever, on both ends.
     */
    @Test
    fun `a timestamp does not go through the counter cap`() {
        val p = ProgressRules.clean(Progress(solved = 5_000_000, updatedAt = now), now)
        assertEquals(ProgressRules.MAX_COUNT, p.solved)
        assertEquals(now, p.updatedAt)
        assertTrue(p.updatedAt > ProgressRules.MAX_COUNT)
    }

    @Test
    fun `a clock running wildly fast is capped a day ahead, not frozen out`() {
        val p = ProgressRules.clean(Progress(updatedAt = now + 400L * 86_400_000L), now)
        assertEquals(now + 86_400_000L, p.updatedAt)
    }

    @Test
    fun `the wire format is the one the website writes`() {
        val json = JSONObject(
            ProgressJson.encode(
                Progress(setOf(0, 2), solved = 4, clean = 3, streak = 3, best = 5, vlsm = 1, analyze = 3, updatedAt = now),
            ),
        )
        // The web stores chapters as index -> timestamp, not as a list.
        assertEquals(setOf("0", "2"), json.getJSONObject("chapters").keys().asSequence().toSet())
        assertEquals(1, json.getJSONObject("byKind").getInt("vlsm"))
        assertEquals(3, json.getJSONObject("byKind").getInt("analyze"))
        assertEquals(now, json.getLong("updatedAt"))
    }

    @Test
    fun `a blob survives the round trip`() {
        val before = Progress(setOf(1, 4, 8), solved = 12, clean = 9, streak = 2, best = 7, vlsm = 5, analyze = 7, updatedAt = now)
        assertEquals(before, ProgressJson.decode(ProgressJson.encode(before), now))
    }

    @Test
    fun `nonsense off the wire decodes to something usable`() {
        assertEquals(Progress(), ProgressJson.decode("not json at all", now))
        assertEquals(Progress(), ProgressJson.decode(null, now))
        assertEquals(Progress(), ProgressJson.decode("", now))

        // Negative counters, a chapter index off the end, a string where a
        // number belongs — all of it is a server or a cache being wrong, and
        // none of it should reach the screen.
        val hostile = ProgressJson.decode(
            """{"chapters":{"-1":1,"400":1,"2":1,"x":1},"solved":-5,"best":"lots","byKind":{"vlsm":1e9}}""",
            now,
        )
        assertEquals(setOf(2), hostile.chapters)
        assertEquals(0, hostile.solved)
        assertEquals(0, hostile.best)
        assertEquals(ProgressRules.MAX_COUNT, hostile.vlsm)
    }

    @Test
    fun `an untouched copy is empty and so is not worth merging`() {
        assertTrue(Progress().isEmpty)
        assertFalse(Progress(solved = 1).isEmpty)
        assertFalse(Progress(chapters = setOf(0)).isEmpty)
        // Somebody who read nothing and solved nothing but has a best from a
        // reset streak still has something worth keeping.
        assertFalse(Progress(best = 3).isEmpty)
    }
}
