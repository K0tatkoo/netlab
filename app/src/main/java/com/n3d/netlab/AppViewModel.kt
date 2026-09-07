package com.n3d.netlab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.n3d.netlab.core.ANALYZE_STAGE_COUNT
import com.n3d.netlab.core.Allocation
import com.n3d.netlab.core.AnalyzeAnswer
import com.n3d.netlab.core.AnalyzeField
import com.n3d.netlab.core.AnalyzeStage
import com.n3d.netlab.core.AnalyzeTask
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.core.FieldState
import com.n3d.netlab.core.Generator
import com.n3d.netlab.core.Grader
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.SubnetAnswer
import com.n3d.netlab.core.VLSM_STAGE_COUNT
import com.n3d.netlab.core.VlsmField
import com.n3d.netlab.core.VlsmStage
import com.n3d.netlab.core.VlsmTask
import com.n3d.netlab.core.fields
import com.n3d.netlab.core.orderedLargestFirst
import com.n3d.netlab.data.Prefs
import com.n3d.netlab.data.Settings
import com.n3d.netlab.data.ThemeMode
import com.n3d.netlab.i18n.Lang
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.stringsFor
import kotlinx.coroutines.launch

/** The two things the app does. Everything else is a sheet over one of them. */
enum class AppMode { Learn, Exercise }

/** Full-screen panels that are not one of the two modes. */
enum class Sheet { None, Calculator, Settings }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app.applicationContext)

    var settings by mutableStateOf(Settings(lang = Lang.En))
        private set

    val strings: Strings get() = stringsFor(settings.lang)

    // ---- navigation ----------------------------------------------------------

    var mode by mutableStateOf(AppMode.Learn)
        private set
    var sheet by mutableStateOf(Sheet.None)
        private set

    /**
     * Named `selectMode` and not `setMode`: `var mode by mutableStateOf` already
     * generates a `setMode` on the JVM, and the two collide.
     */
    fun selectMode(value: AppMode) {
        mode = value
        sheet = Sheet.None
    }

    fun openSheet(value: Sheet) { sheet = value }
    fun closeSheet() { sheet = Sheet.None }

    // ---- exercise state ------------------------------------------------------

    var vlsmTask by mutableStateOf(Generator.vlsm(Difficulty.Easy))
        private set
    var analyzeTask by mutableStateOf(Generator.analyze(Difficulty.Easy))
        private set

    /** The learner's current drag order — the answer to the first stage. */
    var orderDraft by mutableStateOf(vlsmTask.requirements)
        private set

    var vlsmAnswers by mutableStateOf<Map<String, SubnetAnswer>>(emptyMap())
        private set
    var analyzeAnswer by mutableStateOf(AnalyzeAnswer())
        private set

    var vlsmStage by mutableStateOf(VlsmStage.Order)
        private set
    var analyzeStage by mutableStateOf(AnalyzeStage.Mask)
        private set

    /** A check has been run on this stage — until then nothing is marked. */
    var stageChecked by mutableStateOf(false)
        private set

    /** The stage is answered: the next button says Continue, not Check. */
    var stagePassed by mutableStateOf(false)
        private set

    /** The answer was handed over rather than worked out. */
    var stageRevealed by mutableStateOf(false)
        private set

    /** Stages that took more than one attempt, counted once each. */
    var mistakes by mutableStateOf(0)
        private set
    private var stageFailed = false

    var solutionOpen by mutableStateOf(false)
        private set

    /** The finished exercise is recorded once, when the last stage is passed. */
    private var scored = false

    /**
     * DataStore emits its first value asynchronously, so the very first frame
     * runs on defaults. Regenerating the exercise once that arrives is right;
     * regenerating on every later emission would throw away half-typed answers
     * every time a stat is written, which is why this flag exists.
     */
    private var hydrated = false

    init {
        viewModelScope.launch {
            prefs.flow.collect { incoming ->
                val settingsChanged = hydrated &&
                    (incoming.kind != settings.kind || incoming.difficulty != settings.difficulty)
                settings = incoming
                if (!hydrated) {
                    hydrated = true
                    newExercise()
                } else if (settingsChanged) {
                    newExercise()
                }
            }
        }
    }

    // ---- settings ------------------------------------------------------------

    fun setLang(value: Lang) { viewModelScope.launch { prefs.setLang(value) } }
    fun setTheme(value: ThemeMode) { viewModelScope.launch { prefs.setTheme(value) } }
    fun setKind(value: ExerciseKind) { viewModelScope.launch { prefs.setKind(value) } }
    fun setDifficulty(value: Difficulty) { viewModelScope.launch { prefs.setDifficulty(value) } }
    fun resetStats() { viewModelScope.launch { prefs.resetStats() } }

    /** Called by the reader when a chapter's last page comes into view. */
    fun markChapterRead(index: Int) {
        if (index in settings.chaptersRead) return
        viewModelScope.launch { prefs.markChapterRead(index) }
    }

    // ---- exercise flow -------------------------------------------------------

    fun newExercise() {
        vlsmTask = Generator.vlsm(settings.difficulty)
        analyzeTask = Generator.analyze(settings.difficulty)
        orderDraft = vlsmTask.requirements
        vlsmAnswers = emptyMap()
        analyzeAnswer = AnalyzeAnswer()
        vlsmStage = VlsmStage.Order
        analyzeStage = AnalyzeStage.Mask
        mistakes = 0
        scored = false
        solutionOpen = false
        resetStageFlags()
    }

    private fun resetStageFlags() {
        stageChecked = false
        stagePassed = false
        stageRevealed = false
        stageFailed = false
    }

    /** Subnets in allocation order — largest first, which is stage one's answer. */
    val orderedAllocations: List<Allocation> get() = vlsmTask.plan.allocations

    fun answerFor(name: String): SubnetAnswer = vlsmAnswers[name] ?: SubnetAnswer()

    /**
     * Moves one subnet in the drag order.
     *
     * Editing after a failed check clears the verdict rather than leaving a red
     * banner over a list the learner has already changed.
     */
    fun moveOrder(from: Int, to: Int) {
        val list = orderDraft.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        orderDraft = list
        if (!stagePassed) stageChecked = false
    }

    fun updateVlsm(name: String, field: VlsmField, value: String) {
        if (stagePassed) return
        vlsmAnswers = vlsmAnswers + (name to answerFor(name).with(field, value))
        stageChecked = false
    }

    fun updateAnalyze(field: AnalyzeField, value: String) {
        if (stagePassed) return
        analyzeAnswer = analyzeAnswer.with(field, value)
        stageChecked = false
    }

    // ---- grading -------------------------------------------------------------

    val vlsmGrades: Map<String, Map<VlsmField, FieldState>>
        get() = orderedAllocations.associate { alloc ->
            alloc.requirement.name to Grader.grade(answerFor(alloc.requirement.name), alloc)
        }

    val analyzeGrades: Map<AnalyzeField, FieldState>
        get() = Grader.grade(analyzeAnswer, analyzeTask)

    /** Every field the stage on screen is asking for, already marked. */
    private val stageStates: List<FieldState>
        get() = when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmStage.fields.let { fields ->
                orderedAllocations.flatMap { alloc ->
                    val grades = vlsmGrades[alloc.requirement.name].orEmpty()
                    fields.mapNotNull { grades[it] }
                }
            }
            ExerciseKind.Analyze -> analyzeStage.fields.mapNotNull { analyzeGrades[it] }
        }

    /** The order stage has no fields, so it is judged on the drag order alone. */
    private val orderCorrect: Boolean get() = orderedLargestFirst(orderDraft)

    val stageIsOrder: Boolean
        get() = settings.kind == ExerciseKind.Vlsm && vlsmStage == VlsmStage.Order

    val stageEmptyCount: Int get() = if (stageIsOrder) 0 else stageStates.count { it == FieldState.Empty }

    val stageWrongCount: Int get() = if (stageIsOrder) 0 else stageStates.count { it == FieldState.Wrong }

    val stageCorrect: Boolean
        get() = if (stageIsOrder) orderCorrect else stageStates.isNotEmpty() && stageStates.all { it == FieldState.Correct }

    val stageDone: Boolean
        get() = when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmStage == VlsmStage.Done
            ExerciseKind.Analyze -> analyzeStage == AnalyzeStage.Done
        }

    val stageNumber: Int
        get() = when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmStage.index + 1
            ExerciseKind.Analyze -> analyzeStage.index + 1
        }

    val stageTotal: Int
        get() = when (settings.kind) {
            ExerciseKind.Vlsm -> VLSM_STAGE_COUNT
            ExerciseKind.Analyze -> ANALYZE_STAGE_COUNT
        }

    // ---- stage transitions ---------------------------------------------------

    fun checkStage() {
        if (stagePassed || stageDone) return
        stageChecked = true
        if (stageCorrect) {
            stagePassed = true
        } else if (!stageFailed) {
            // Counted once per stage: three attempts at the same step is one
            // thing gone wrong, not three.
            stageFailed = true
            mistakes++
        }
    }

    /** Fills the stage in and marks it as help taken, without advancing. */
    fun revealStage() {
        if (stagePassed || stageDone) return
        when (settings.kind) {
            ExerciseKind.Vlsm -> fillVlsmStage()
            ExerciseKind.Analyze -> fillAnalyzeStage()
        }
        if (!stageFailed) {
            stageFailed = true
            mistakes++
        }
        stageRevealed = true
        stageChecked = true
        stagePassed = true
    }

    private fun fillVlsmStage() {
        when (vlsmStage) {
            VlsmStage.Order -> orderDraft = orderedAllocations.map { it.requirement }
            VlsmStage.Size -> vlsmAnswers = orderedAllocations.associate { alloc ->
                alloc.requirement.name to answerFor(alloc.requirement.name)
                    .copy(prefix = "/${alloc.prefix}")
            }
            VlsmStage.Place -> vlsmAnswers = orderedAllocations.associate { alloc ->
                alloc.requirement.name to answerFor(alloc.requirement.name).copy(
                    network = Ip.format(alloc.network),
                    firstHost = Ip.format(alloc.firstHost),
                    lastHost = Ip.format(alloc.lastHost),
                    broadcast = Ip.format(alloc.broadcast),
                )
            }
            VlsmStage.Done -> Unit
        }
    }

    private fun fillAnalyzeStage() {
        var answer = analyzeAnswer
        analyzeStage.fields.forEach { field ->
            answer = answer.with(field, Grader.expected(field, analyzeTask))
        }
        analyzeAnswer = answer
    }

    fun continueStage() {
        if (!stagePassed) return
        when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmStage = when (vlsmStage) {
                VlsmStage.Order -> VlsmStage.Size
                VlsmStage.Size -> VlsmStage.Place
                else -> VlsmStage.Done
            }
            ExerciseKind.Analyze -> analyzeStage = when (analyzeStage) {
                AnalyzeStage.Mask -> AnalyzeStage.Network
                AnalyzeStage.Network -> AnalyzeStage.Broadcast
                AnalyzeStage.Broadcast -> AnalyzeStage.Hosts
                else -> AnalyzeStage.Done
            }
        }
        resetStageFlags()
        if (stageDone && !scored) {
            scored = true
            val clean = mistakes == 0
            val kind = settings.kind
            viewModelScope.launch { prefs.recordResult(kind, clean) }
        }
    }

    /** Steps back to re-read a stage that is already answered. */
    fun previousStage() {
        when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmStage = when (vlsmStage) {
                VlsmStage.Done -> VlsmStage.Place
                VlsmStage.Place -> VlsmStage.Size
                VlsmStage.Size -> VlsmStage.Order
                VlsmStage.Order -> return
            }
            ExerciseKind.Analyze -> analyzeStage = when (analyzeStage) {
                AnalyzeStage.Done -> AnalyzeStage.Hosts
                AnalyzeStage.Hosts -> AnalyzeStage.Broadcast
                AnalyzeStage.Broadcast -> AnalyzeStage.Network
                AnalyzeStage.Network -> AnalyzeStage.Mask
                AnalyzeStage.Mask -> return
            }
        }
        // Going back lands on a stage that is already right, so it opens
        // answered rather than blank and waiting to be checked again.
        stageChecked = true
        stagePassed = true
        stageRevealed = false
        stageFailed = true
    }

    fun openSolution() { solutionOpen = true }
    fun closeSolution() { solutionOpen = false }

    /** The task the user is actually looking at, for the walkthrough sheet. */
    val currentAnalyzeTask: AnalyzeTask get() = analyzeTask
    val currentVlsmTask: VlsmTask get() = vlsmTask
}
