package com.n3d.netlab

import com.n3d.netlab.core.AnalyzeTask
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.Generator
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.core.Vlsm
import com.n3d.netlab.core.steps
import com.n3d.netlab.i18n.Cs
import com.n3d.netlab.i18n.En
import com.n3d.netlab.i18n.Lang
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.stringsFor
import com.n3d.netlab.i18n.render
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The walkthrough is the feature the app exists for and it is generated text,
 * so the risk is not a wrong number but a crash — an index off the end of an
 * octet, a `!!` on a /31's absent first host, a division by a zero magic
 * number. This renders every step of a few thousand tasks in both languages
 * and insists nothing is empty and nothing throws.
 */
class WalkthroughTest {

    private val languages = listOf(En, Cs)

    private fun assertReadable(text: String, where: String) {
        assertFalse("$where produced blank text", text.isBlank())
        // A failed interpolation shows up as a literal placeholder, never as
        // an exception, so it has to be asserted rather than caught.
        assertFalse("$where leaked a placeholder: $text", text.contains("null"))
        assertFalse("$where leaked a template: $text", text.contains("\${"))
    }

    @Test
    fun `every vlsm step renders in both languages`() {
        val random = Random(1312)
        languages.forEach { s ->
            Difficulty.entries.forEach { difficulty ->
                repeat(120) {
                    val cards = Generator.vlsm(difficulty, random).plan.steps().map { render(it, s) }
                    assertTrue(cards.size >= 4)
                    cards.forEach { card ->
                        assertReadable(card.title, "${s.lang} title")
                        assertTrue("empty body", card.body.isNotEmpty())
                        card.body.forEach { assertReadable(it, "${s.lang} body") }
                        card.rows.forEach { (label, value) ->
                            assertReadable(label, "${s.lang} row label")
                            assertReadable(value, "${s.lang} row value")
                        }
                        card.formula?.let { assertReadable(it, "${s.lang} formula") }
                        card.bits?.let { assertEquals(32, it.bits.length) }
                    }
                }
            }
        }
    }

    @Test
    fun `every analyse step renders in both languages`() {
        val random = Random(99)
        languages.forEach { s ->
            Difficulty.entries.forEach { difficulty ->
                repeat(120) {
                    val cards = Generator.analyze(difficulty, random).steps().map { render(it, s) }
                    assertEquals(6, cards.size)
                    cards.forEach { card ->
                        assertReadable(card.title, "${s.lang} title")
                        card.body.forEach { assertReadable(it, "${s.lang} body") }
                        card.rows.forEach { (label, value) ->
                            assertReadable(label, "${s.lang} row label")
                            assertReadable(value, "${s.lang} row value")
                        }
                    }
                }
            }
        }
    }

    /**
     * The generator never produces these, but the calculator hands the same
     * renderer whatever the user drags the sliders to.
     */
    @Test
    fun `renders the edge prefixes the calculator can reach`() {
        languages.forEach { s ->
            for (prefix in 0..32) {
                val task = AnalyzeTask(Ip.parse("10.11.12.13")!!, prefix)
                task.steps().map { render(it, s) }.forEach { card ->
                    assertReadable(card.title, "/$prefix ${s.lang}")
                    card.body.forEach { assertReadable(it, "/$prefix ${s.lang} body") }
                }
            }
        }
    }

    @Test
    fun `renders a plan that does not fit`() {
        languages.forEach { s ->
            val plan = Vlsm.plan(
                Ip.parse("192.168.1.0")!!,
                24,
                listOf(Requirement("A", 200), Requirement("B", 100), Requirement("C", 4000)),
            )
            assertFalse(plan.fits)
            val cards = plan.steps().map { render(it, s) }
            cards.forEach { card ->
                assertReadable(card.title, "overflow ${s.lang}")
                card.body.forEach { assertReadable(it, "overflow ${s.lang} body") }
            }
        }
    }

    @Test
    fun `czech declines the plural forms it needs`() {
        assertEquals("1 uzel", Cs.hosts(1))
        assertEquals("3 uzly", Cs.hosts(3))
        assertEquals("60 uzlů", Cs.hosts(60))
        assertEquals("1 adresa", Cs.addresses(1))
        assertEquals("2 adresy", Cs.addresses(2))
        assertEquals("64 adres", Cs.addresses(64))
        assertEquals("1 podsíť", Cs.subnets(1))
        assertEquals("4 podsítě", Cs.subnets(4))
        assertEquals("6 podsítí", Cs.subnets(6))
        assertEquals("1 bit", Cs.bits(1))
        assertEquals("2 bity", Cs.bits(2))
        assertEquals("6 bitů", Cs.bits(6))
    }

    @Test
    fun `both languages carry the same lessons and are reachable by tag`() {
        assertEquals(En.lessons.size, Cs.lessons.size)
        assertEquals(En, stringsFor(Lang.En))
        assertEquals(Cs, stringsFor(Lang.Cs))
        listOf<Strings>(En, Cs).forEach { s ->
            s.lessons.forEach { lesson ->
                assertReadable(lesson.title, "${s.lang} lesson title")
                assertReadable(lesson.summary, "${s.lang} lesson summary")
                assertTrue("${s.lang}: ${lesson.title} has no body", lesson.body.isNotEmpty())
            }
        }
    }
}
