package com.n3d.netlab

import com.n3d.netlab.core.AnalyzeTask
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.Generator
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.core.AnalyzeStage
import com.n3d.netlab.core.Vlsm
import com.n3d.netlab.core.VlsmStage
import com.n3d.netlab.core.steps
import com.n3d.netlab.i18n.Block
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
    fun `the two courses are structurally identical`() {
        assertEquals(En, stringsFor(Lang.En))
        assertEquals(Cs, stringsFor(Lang.Cs))
        assertEquals("chapter count", En.course.size, Cs.course.size)

        En.course.zip(Cs.course).forEachIndexed { chapterIndex, (en, cs) ->
            assertEquals(
                "chapter ${chapterIndex + 1} page count",
                en.pages.size,
                cs.pages.size,
            )
            en.pages.zip(cs.pages).forEachIndexed { pageIndex, (enPage, csPage) ->
                val where = "chapter ${chapterIndex + 1} page ${pageIndex + 1}"
                // Block *types*, in order. This is the invariant that keeps the
                // two languages one course: a paragraph added to the English
                // page and not to the Czech one would otherwise go unnoticed
                // until a Czech reader hit a worked example with a step missing.
                assertEquals(
                    "$where block shapes",
                    enPage.blocks.map { it::class.simpleName },
                    csPage.blocks.map { it::class.simpleName },
                )
            }
        }
    }

    @Test
    fun `every course page is readable in both languages`() {
        languages.forEach { s ->
            assertReadable(s.learnIntro, "${s.lang} learn intro")
            s.course.forEachIndexed { chapterIndex, chapter ->
                assertReadable(chapter.title, "${s.lang} chapter title")
                assertReadable(chapter.summary, "${s.lang} chapter summary")
                assertTrue("${s.lang}: ${chapter.title} has no pages", chapter.pages.isNotEmpty())
                chapter.pages.forEach { page ->
                    val where = "${s.lang} ch${chapterIndex + 1} ${page.title}"
                    assertReadable(page.title, "$where title")
                    assertTrue("$where has no blocks", page.blocks.isNotEmpty())
                    page.blocks.forEach { block -> assertBlockReadable(block, where) }
                }
            }
        }
    }

    private fun assertBlockReadable(block: Block, where: String) {
        when (block) {
            is Block.Para -> assertReadable(block.text, "$where para")
            is Block.Heading -> assertReadable(block.text, "$where heading")
            is Block.Formula -> assertReadable(block.text, "$where formula")
            is Block.Note -> assertReadable(block.text, "$where note")
            is Block.Tip -> assertReadable(block.text, "$where tip")
            is Block.Bullets -> {
                assertTrue("$where empty bullets", block.items.isNotEmpty())
                block.items.forEach { assertReadable(it, "$where bullet") }
            }
            is Block.Table -> {
                assertTrue("$where empty table", block.rows.isNotEmpty())
                block.rows.forEach { (label, value) ->
                    assertReadable(label, "$where table label")
                    assertReadable(value, "$where table value")
                }
            }
            is Block.Bits -> {
                assertReadable(block.caption, "$where bits caption")
                assertEquals("$where bit string length", 32, block.bits.length)
                assertTrue("$where prefix out of range", block.networkBits in 0..32)
            }
            is Block.Work -> {
                assertTrue("$where empty work", block.lines.any { it.isNotBlank() })
                // Blank lines are the paragraph breaks of a worked calculation,
                // so they are legal here and only the non-blank ones are text.
                block.lines.filter { it.isNotBlank() }
                    .forEach { assertReadable(it, "$where work line") }
            }
            is Block.Recipe -> {
                assertTrue("$where empty recipe", block.items.isNotEmpty())
                block.items.forEach { step ->
                    assertReadable(step.rule, "$where recipe rule")
                    step.work?.let { assertReadable(it, "$where recipe work") }
                }
            }
            is Block.Split -> {
                assertReadable(block.caption, "$where split caption")
                assertTrue("$where empty split", block.parts.isNotEmpty())
                val total = block.parts.sumOf { it.size }
                // A bar drawn to scale is only honest if the parts really do
                // add up to the network being drawn.
                assertEquals("$where split does not fill its capacity", block.capacity, total)
                block.parts.forEach { assertReadable(it.label, "$where split label") }
            }
            is Block.PlaceValue -> {
                assertReadable(block.caption, "$where place value caption")
                assertTrue("$where octet out of range", block.value in 0..255)
            }
            is Block.Check -> {
                assertReadable(block.question, "$where check question")
                assertReadable(block.answer, "$where check answer")
                block.why?.let { assertReadable(it, "$where check why") }
            }
        }
    }

    @Test
    fun `every stage has a title, a prompt and a hint in both languages`() {
        languages.forEach { s ->
            VlsmStage.entries.forEach { stage ->
                assertReadable(s.vlsmStageTitle(stage), "${s.lang} vlsm stage title")
                assertReadable(s.vlsmStagePrompt(stage), "${s.lang} vlsm stage prompt")
                if (stage != VlsmStage.Done) {
                    assertTrue("${s.lang} $stage has no hint", s.vlsmStageHint(stage).isNotEmpty())
                    s.vlsmStageHint(stage).forEach { assertReadable(it, "${s.lang} vlsm hint") }
                }
            }
            AnalyzeStage.entries.forEach { stage ->
                assertReadable(s.analyzeStageTitle(stage), "${s.lang} analyze stage title")
                assertReadable(s.analyzeStagePrompt(stage), "${s.lang} analyze stage prompt")
                if (stage != AnalyzeStage.Done) {
                    assertTrue("${s.lang} $stage has no hint", s.analyzeStageHint(stage).isNotEmpty())
                    s.analyzeStageHint(stage).forEach { assertReadable(it, "${s.lang} analyze hint") }
                }
            }
        }
    }
}
