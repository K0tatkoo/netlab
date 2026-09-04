package com.n3d.netlab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.n3d.netlab.core.AnalyzeAnswer
import com.n3d.netlab.core.AnalyzeField
import com.n3d.netlab.core.AnalyzeTask
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.core.FieldState
import com.n3d.netlab.core.Generator
import com.n3d.netlab.core.Grader
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.SubnetAnswer
import com.n3d.netlab.core.VlsmField
import com.n3d.netlab.core.VlsmTask
import com.n3d.netlab.data.Prefs
import com.n3d.netlab.data.Settings
import com.n3d.netlab.data.ThemeMode
import com.n3d.netlab.i18n.Lang
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.stringsFor
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app.applicationContext)

    var settings by mutableStateOf(Settings(lang = Lang.En))
        private set

    val strings: Strings get() = stringsFor(settings.lang)

    // ---- practice state ------------------------------------------------------

    var vlsmTask by mutableStateOf(Generator.vlsm(Difficulty.Easy))
        private set
    var analyzeTask by mutableStateOf(Generator.analyze(Difficulty.Easy))
        private set
    var vlsmAnswers by mutableStateOf<Map<String, SubnetAnswer>>(emptyMap())
        private set
    var analyzeAnswer by mutableStateOf(AnalyzeAnswer())
        private set
    var checked by mutableStateOf(false)
        private set
    var solutionOpen by mutableStateOf(false)
        private set
    /** Stats are recorded once per exercise, on the first Check. */
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

    // ---- which fields the exercise asks for ----------------------------------

    val vlsmFields: List<VlsmField>
        get() = if (settings.shortFields) {
            listOf(VlsmField.Network, VlsmField.Prefix)
        } else {
            VlsmField.entries
        }

    val analyzeFields: List<AnalyzeField>
        get() = if (settings.shortFields) {
            listOf(AnalyzeField.Network, AnalyzeField.Mask, AnalyzeField.Broadcast)
        } else {
            AnalyzeField.entries
        }

    // ---- settings ------------------------------------------------------------

    fun setLang(value: Lang) { viewModelScope.launch { prefs.setLang(value) } }
    fun setTheme(value: ThemeMode) { viewModelScope.launch { prefs.setTheme(value) } }
    fun setKind(value: ExerciseKind) { viewModelScope.launch { prefs.setKind(value) } }
    fun setDifficulty(value: Difficulty) { viewModelScope.launch { prefs.setDifficulty(value) } }
    fun setShortFields(value: Boolean) { viewModelScope.launch { prefs.setShortFields(value) } }
    fun resetStats() { viewModelScope.launch { prefs.resetStats() } }

    // ---- exercise flow -------------------------------------------------------

    fun newExercise() {
        vlsmTask = Generator.vlsm(settings.difficulty)
        analyzeTask = Generator.analyze(settings.difficulty)
        vlsmAnswers = emptyMap()
        analyzeAnswer = AnalyzeAnswer()
        checked = false
        scored = false
        solutionOpen = false
    }

    fun answerFor(name: String): SubnetAnswer = vlsmAnswers[name] ?: SubnetAnswer()

    fun updateVlsm(name: String, field: VlsmField, value: String) {
        vlsmAnswers = vlsmAnswers + (name to answerFor(name).with(field, value))
        checked = false
    }

    fun updateAnalyze(field: AnalyzeField, value: String) {
        analyzeAnswer = analyzeAnswer.with(field, value)
        checked = false
    }

    fun clearAnswers() {
        vlsmAnswers = emptyMap()
        analyzeAnswer = AnalyzeAnswer()
        checked = false
    }

    /** Drops the correct answers into the fields — for reading the solution back. */
    fun fillCorrect() {
        when (settings.kind) {
            ExerciseKind.Vlsm -> {
                vlsmAnswers = vlsmTask.plan.allocations.associate { alloc ->
                    alloc.requirement.name to SubnetAnswer(
                        network = Ip.format(alloc.network),
                        prefix = "/${alloc.prefix}",
                        firstHost = Ip.format(alloc.firstHost),
                        lastHost = Ip.format(alloc.lastHost),
                        broadcast = Ip.format(alloc.broadcast),
                    )
                }
            }
            ExerciseKind.Analyze -> {
                val t = analyzeTask
                analyzeAnswer = AnalyzeAnswer(
                    network = Ip.format(t.network),
                    mask = Ip.format(t.mask),
                    firstHost = t.firstHost?.let { Ip.format(it) } ?: "",
                    lastHost = t.lastHost?.let { Ip.format(it) } ?: "",
                    broadcast = Ip.format(t.broadcast),
                    hosts = t.usable.toString(),
                )
            }
        }
        // Filling the answers in is not solving it, so the result never counts.
        scored = true
        checked = false
    }

    fun check() {
        checked = true
        if (!scored && !isBlank) {
            scored = true
            val correct = allCorrect
            viewModelScope.launch { prefs.recordResult(correct) }
        }
    }

    fun openSolution() {
        solutionOpen = true
    }

    fun closeSolution() {
        solutionOpen = false
    }

    // ---- grading -------------------------------------------------------------

    val vlsmGrades: Map<String, Map<VlsmField, FieldState>>
        get() = vlsmTask.plan.allocations.associate { alloc ->
            alloc.requirement.name to Grader.grade(answerFor(alloc.requirement.name), alloc)
        }

    val analyzeGrades: Map<AnalyzeField, FieldState>
        get() = Grader.grade(analyzeAnswer, analyzeTask)

    private val askedStates: List<FieldState>
        get() = when (settings.kind) {
            ExerciseKind.Vlsm -> vlsmGrades.values.flatMap { grades ->
                vlsmFields.mapNotNull { grades[it] }
            }
            ExerciseKind.Analyze -> analyzeFields.mapNotNull { analyzeGrades[it] }
        }

    val isBlank: Boolean
        get() = askedStates.all { it == FieldState.Empty }

    val allCorrect: Boolean
        get() = askedStates.isNotEmpty() && askedStates.all { it == FieldState.Correct }

    val wrongCount: Int
        get() = askedStates.count { it == FieldState.Wrong }

    val emptyCount: Int
        get() = askedStates.count { it == FieldState.Empty }

    val askedCount: Int
        get() = askedStates.size

    /** The task the user is actually looking at, for the walkthrough sheet. */
    val currentAnalyzeTask: AnalyzeTask get() = analyzeTask
    val currentVlsmTask: VlsmTask get() = vlsmTask
}
