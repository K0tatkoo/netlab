package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStage
import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.VlsmStage
import com.n3d.netlab.core.VlsmStep

enum class Lang(val code: String, val flag: String) {
    En("en", "EN"),
    Cs("cs", "CS"),
}

fun stringsFor(lang: Lang): Strings = when (lang) {
    Lang.En -> En
    Lang.Cs -> Cs
}

// ---------------------------------------------------------------------------
// The course
// ---------------------------------------------------------------------------

/**
 * One screenful of the course — one idea, read and then paged past.
 *
 * A page rather than a scrolling chapter because the material is cumulative:
 * page 4 assumes page 3, and a reader who can scroll past three screens of
 * binary in one flick will do exactly that and then not understand the mask.
 */
data class Page(val title: String, val blocks: List<Block>)

data class Chapter(val title: String, val summary: String, val pages: List<Page>)

sealed interface Block {
    data class Para(val text: String) : Block

    /** A sub-heading inside a page. */
    data class Heading(val text: String) : Block

    data class Bullets(val items: List<String>) : Block

    data class Table(val rows: List<Pair<String, String>>) : Block

    /** One line of arithmetic, centred and set apart. */
    data class Formula(val text: String) : Block

    /** A caveat or exam trap. */
    data class Note(val text: String) : Block

    /** A shortcut worth keeping — the same shape as [Note], a friendlier colour. */
    data class Tip(val text: String) : Block

    /** A 32-bit strip with the first `networkBits` bits tinted. */
    data class Bits(val caption: String, val bits: String, val networkBits: Int) : Block

    /**
     * A worked calculation, left-aligned and monospaced over several lines.
     *
     * This is the block the whole Learn section exists for: the arithmetic
     * written out the way it would be on paper, one operation per line, with
     * nothing skipped because it is "obvious".
     */
    data class Work(val caption: String?, val lines: List<String>) : Block

    /** A numbered procedure. Each step is a general rule; the work is optional. */
    data class Recipe(val items: List<RecipeStep>) : Block

    /** A network drawn to scale and split into labelled blocks. */
    data class Split(val caption: String, val capacity: Long, val parts: List<SplitPart>) : Block

    /** The 128 64 32 16 8 4 2 1 columns, with one octet's bits switched on. */
    data class PlaceValue(val caption: String, val value: Int) : Block

    /** A question the reader answers in their head, then taps to check. */
    data class Check(val question: String, val answer: String, val why: String? = null) : Block
}

/**
 * `rule` is the sentence that is true for every exercise; `work` is what it
 * looks like on this one. Keeping them in one object is what makes the
 * walkthrough teach a method rather than reveal an answer.
 */
data class RecipeStep(val rule: String, val work: String? = null)

data class SplitPart(val label: String, val size: Long, val detail: String, val free: Boolean = false)

/**
 * Every user-visible word in the app.
 *
 * This is an interface rather than a string-resource lookup for two reasons.
 * The language switch has to take effect on the current screen without
 * restarting the activity, which `values-cs/` cannot do without recreating the
 * whole task; and an abstract member for every single string means a missing
 * Czech translation is a compile error rather than an English word appearing
 * mid-sentence on someone's phone.
 */
interface Strings {

    val lang: Lang

    // ---- formatting ----------------------------------------------------------

    /** Thousands separator — a comma in English, a thin space in Czech. */
    fun num(value: Long): String
    fun num(value: Int): String = num(value.toLong())

    /** "60 hosts" / "60 uzlů" — declined correctly in Czech. */
    fun hosts(count: Long): String
    fun addresses(count: Long): String
    fun subnets(count: Int): String
    fun bits(count: Int): String

    // ---- navigation ----------------------------------------------------------

    val appName: String
    val tabLearn: String
    val tabExercise: String
    val tabCalculator: String
    val tabSettings: String

    // ---- shared vocabulary ---------------------------------------------------

    val actionCheck: String
    val actionSolution: String
    val actionNewExercise: String
    val actionNext: String
    val actionBack: String
    val actionClose: String
    val actionAdd: String
    val actionShowAll: String
    val actionOneByOne: String
    val actionClear: String
    val actionConfirm: String
    val actionCancel: String
    val actionContinue: String
    val actionRetry: String
    val actionHint: String
    val actionHideHint: String
    val actionShowAnswer: String
    /** Uncovers the answer to a Check block in the course. */
    val actionReveal: String

    val labelExpected: String
    val labelSolved: String
    val labelStreak: String
    val labelBest: String
    val labelDifficulty: String
    val diffEasy: String
    val diffMedium: String
    val diffHard: String

    val fieldNetwork: String
    val fieldPrefix: String
    val fieldMask: String
    val fieldFirstHost: String
    val fieldLastHost: String
    val fieldBroadcast: String
    val fieldUsableHosts: String
    val fieldWildcard: String
    val fieldBlockSize: String
    val fieldHostBits: String
    val fieldNetworkBits: String
    val fieldRange: String
    val fieldTotalAddresses: String
    val fieldClass: String
    val fieldScope: String
    val fieldBinary: String
    val fieldSpare: String
    val fieldAddress: String

    val classA: String
    val classB: String
    val classC: String
    val classD: String
    val classE: String
    val classLoopback: String
    val classThisNetwork: String

    val scopePrivate: String
    val scopePublic: String
    val scopeLoopback: String
    val scopeLinkLocal: String
    val scopeCgnat: String
    val scopeMulticast: String
    val scopeReserved: String

    // ---- exercise ------------------------------------------------------------

    val exerciseTitle: String
    val kindVlsm: String
    val kindAnalyze: String
    val kindVlsmHint: String
    val kindAnalyzeHint: String

    val assignment: String
    val baseNetwork: String
    val givenAddress: String
    val vlsmPrompt: String
    val analyzePrompt: String
    val yourAnswer: String

    fun subnetTitle(name: String): String
    fun stageOf(index: Int, total: Int): String

    /** Stage headings and the sentence that says what to do on them. */
    fun vlsmStageTitle(stage: VlsmStage): String
    fun vlsmStagePrompt(stage: VlsmStage): String
    fun vlsmStageHint(stage: VlsmStage): List<String>
    fun analyzeStageTitle(stage: AnalyzeStage): String
    fun analyzeStagePrompt(stage: AnalyzeStage): String
    fun analyzeStageHint(stage: AnalyzeStage): List<String>

    val hintTitle: String
    val dragHandle: String
    val orderPrompt: String
    val orderTopLabel: String
    val orderBottomLabel: String

    val stageCorrect: String
    val stageWrongOrder: String
    fun stageWrongFields(wrong: Int): String
    val stageIncomplete: String
    val stageRevealed: String

    val resultTitle: String
    val resultPerfect: String
    fun resultMistakes(count: Int): String
    val resultPlan: String
    val resultFree: String

    // ---- walkthrough ---------------------------------------------------------

    val solutionTitle: String
    val ruleLabel: String
    fun stepOf(index: Int, total: Int): String

    val stepIntroTitle: String
    fun stepIntroBody(step: VlsmStep.Intro): List<String>
    val stepIntroRule: String

    val stepOrderTitle: String
    fun stepOrderBody(step: VlsmStep.Order): List<String>
    val stepOrderRule: String

    fun stepSizeTitle(step: VlsmStep.Size): String
    fun stepSizeBody(step: VlsmStep.Size): List<String>
    val stepSizeRule: String

    fun stepPlaceTitle(step: VlsmStep.Place): String
    fun stepPlaceBody(step: VlsmStep.Place): List<String>
    val stepPlaceRule: String

    fun stepOverflowTitle(step: VlsmStep.Overflow): String
    fun stepOverflowBody(step: VlsmStep.Overflow): List<String>
    val stepOverflowRule: String

    val stepVerifyTitle: String
    fun stepVerifyBody(step: VlsmStep.Verify): List<String>
    val stepVerifyRule: String

    fun analyzeStepTitle(step: AnalyzeStep): String
    fun analyzeStepBody(step: AnalyzeStep): List<String>
    fun analyzeStepRule(step: AnalyzeStep): String

    /** Caption over the 32-bit strip: "first 26 bits = network". */
    fun bitsCaption(networkBits: Int): String
    val bitsLegendNetwork: String
    val bitsLegendHost: String

    // ---- rows inside the walkthrough and the calculator -----------------------

    val rowSubnetsRequired: String
    val rowHostsRequested: String
    val rowAddressesNeeded: String
    val rowHostsRequired: String
    val rowPlusTwo: String
    val rowFirstFree: String
    val rowNextFree: String
    val rowUsed: String
    val rowFree: String
    val rowWasted: String
    val rowMagicOctet: String
    val rowMagicNumber: String
    val rowRoundedDown: String

    // ---- calculator ----------------------------------------------------------

    val calculatorTitle: String
    val calcModeSubnet: String
    val calcModeDesigner: String
    val calcInvalidAddress: String
    val calcEnterAddress: String
    val designerBase: String
    val designerSubnets: String
    val designerAddSubnet: String
    val designerRemove: String
    val designerEmpty: String
    fun designerFits(used: Long, capacity: Long): String
    fun designerOverflow(count: Int): String
    val designerPlan: String
    val designerHostsFor: String
    val designerTypeHosts: String

    // ---- learn ---------------------------------------------------------------

    val learnTitle: String
    val learnIntro: String
    val learnStartHere: String
    val course: List<Chapter>
    fun chapterOf(index: Int, total: Int): String
    fun pageOf(index: Int, total: Int): String
    /** Badge on a chapter the reader has already paged to the end of. */
    val chapterRead: String
    val chapterDone: String
    val chapterDoneBody: String
    val checkYourself: String

    // ---- settings ------------------------------------------------------------

    val settingsTitle: String
    val settingLanguage: String
    val settingTheme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val settingDefaults: String
    val settingDefaultKind: String
    val settingDefaultDifficulty: String
    val progressTitle: String
    val progressChapters: String
    val progressExercises: String
    val progressClean: String
    val progressNone: String
    val settingStats: String
    val settingResetStats: String
    val settingResetStatsHint: String
    val settingAbout: String
    val aboutBody: String
    val languageEnglish: String
    val languageCzech: String
}
