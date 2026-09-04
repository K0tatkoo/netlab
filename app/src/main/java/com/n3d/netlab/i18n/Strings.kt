package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.VlsmStep

enum class Lang(val code: String, val flag: String) {
    En("en", "EN"),
    Cs("cs", "CS"),
}

fun stringsFor(lang: Lang): Strings = when (lang) {
    Lang.En -> En
    Lang.Cs -> Cs
}

/** A lesson in the Learn tab. */
data class Lesson(val title: String, val summary: String, val body: List<Block>)

sealed interface Block {
    data class Para(val text: String) : Block
    data class Bullets(val items: List<String>) : Block
    data class Table(val rows: List<Pair<String, String>>) : Block
    /** Monospaced, highlighted — one line of arithmetic. */
    data class Formula(val text: String) : Block
    /** A caveat or exam-trap warning. */
    data class Note(val text: String) : Block
    /** A 32-bit strip drawn with the first `networkBits` bits tinted. */
    data class Bits(val caption: String, val bits: String, val networkBits: Int) : Block
}

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

    val tabPractice: String
    val tabCalculator: String
    val tabLearn: String
    val tabSettings: String

    // ---- shared vocabulary ---------------------------------------------------

    val actionCheck: String
    val actionSolution: String
    val actionHideSolution: String
    val actionNewExercise: String
    val actionNext: String
    val actionBack: String
    val actionReset: String
    val actionClose: String
    val actionAdd: String
    val actionShowAll: String
    val actionOneByOne: String
    val actionFillCorrect: String
    val actionClear: String
    val actionConfirm: String
    val actionCancel: String

    val labelCorrect: String
    val labelWrong: String
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

    // ---- practice ------------------------------------------------------------

    val practiceTitle: String
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

    fun requirement(name: String, hostCount: Int): String
    fun subnetTitle(name: String): String

    val verdictPerfect: String
    fun verdictWrong(wrong: Int, total: Int): String
    val verdictIncomplete: String
    val verdictNothing: String
    val notCheckedYet: String

    // ---- walkthrough ---------------------------------------------------------

    val solutionTitle: String
    fun stepOf(index: Int, total: Int): String

    val stepIntroTitle: String
    fun stepIntroBody(step: VlsmStep.Intro): List<String>

    val stepOrderTitle: String
    fun stepOrderBody(step: VlsmStep.Order): List<String>

    fun stepSizeTitle(step: VlsmStep.Size): String
    fun stepSizeBody(step: VlsmStep.Size): List<String>

    fun stepPlaceTitle(step: VlsmStep.Place): String
    fun stepPlaceBody(step: VlsmStep.Place): List<String>

    fun stepOverflowTitle(step: VlsmStep.Overflow): String
    fun stepOverflowBody(step: VlsmStep.Overflow): List<String>

    val stepVerifyTitle: String
    fun stepVerifyBody(step: VlsmStep.Verify): List<String>

    fun analyzeStepTitle(step: AnalyzeStep): String
    fun analyzeStepBody(step: AnalyzeStep): List<String>

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

    // ---- learn ---------------------------------------------------------------

    val learnTitle: String
    val lessons: List<Lesson>

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
    val settingShortFields: String
    val settingShortFieldsHint: String
    val settingStats: String
    val settingResetStats: String
    val settingResetStatsHint: String
    val settingAbout: String
    val aboutBody: String
    val languageEnglish: String
    val languageCzech: String
}
