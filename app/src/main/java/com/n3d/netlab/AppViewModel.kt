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
import com.n3d.netlab.data.Account
import com.n3d.netlab.data.ApiException
import com.n3d.netlab.data.NetLabApi
import com.n3d.netlab.data.Prefs
import com.n3d.netlab.data.Progress
import com.n3d.netlab.data.ProgressRules
import com.n3d.netlab.data.Settings
import com.n3d.netlab.data.ThemeMode
import com.n3d.netlab.i18n.Lang
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.stringsFor
import com.n3d.netlab.update.UpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The two things the app does. Everything else is a sheet over one of them. */
enum class AppMode { Learn, Exercise }

/** Full-screen panels that are not one of the two modes. */
enum class Sheet { None, Calculator, Settings }

/** Which account form the Settings screen is showing. */
enum class AuthMode { SignIn, Register, Forgot }

/**
 * Something that went well enough to say so. Kept as a value rather than a
 * finished sentence so it follows the language switch like everything else.
 */
enum class AuthNotice { SignedOut, SignedIn, CodeResent, ResetSent }

private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
private const val SYNC_DEBOUNCE_MS = 900L

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app.applicationContext)

    /**
     * The app looking after its own version. See update/Updater.kt.
     *
     * The launch check is silent and rate-limited to once a day: if there is
     * nothing new, or the phone is offline, nothing about this screen changes.
     */
    val updates = UpdateManager(app).also { it.checkOnLaunch() }

    /** Reads the token out of state on every call, so a fresh cookie is picked
     *  up without rebuilding the client. */
    private val api = NetLabApi { sessionToken }

    var settings by mutableStateOf(Settings(lang = Lang.En))
        private set

    // ---- account -------------------------------------------------------------

    private var sessionToken: String? = null

    /** Null while signed out, which is a perfectly normal way to use the app. */
    var account by mutableStateOf<Account?>(null)
        private set

    var progress by mutableStateOf(Progress())
        private set

    var authMode by mutableStateOf(AuthMode.SignIn)
        private set
    var authBusy by mutableStateOf(false)
        private set

    /** A server error code — `credentials`, `rate`, `network` — not a sentence. */
    var authError by mutableStateOf<String?>(null)
        private set
    var authNotice by mutableStateOf<AuthNotice?>(null)
        private set

    /** The half-finished sign-in: a code has been emailed and not yet typed. */
    var challenge by mutableStateOf<String?>(null)
        private set
    var challengeEmail by mutableStateOf("")
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
            prefs.flow.collect { stored ->
                val settingsChanged = hydrated &&
                    (stored.settings.kind != settings.kind || stored.settings.difficulty != settings.difficulty)
                settings = stored.settings
                sessionToken = stored.token
                account = stored.account
                progress = if (stored.account != null) stored.cached else stored.guest
                if (!hydrated) {
                    hydrated = true
                    newExercise()
                    // After the first paint's worth of state, never before: the
                    // app is completely usable signed out, so nothing waits on
                    // a network that may not be there.
                    restoreSession()
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

    // ---- progress ------------------------------------------------------------

    /**
     * Local first, server second, and that order is deliberate: somebody who
     * never signs in still keeps their chapters and their streak, and somebody
     * who does but whose signal drops still gets credited for the exercise they
     * just finished.
     */
    private fun updateProgress(mutate: (Progress) -> Progress) {
        val next = mutate(progress).copy(updatedAt = System.currentTimeMillis())
        if (next == progress) return
        // Set here rather than waiting for the store to echo it back, so a
        // ticked chapter appears the instant it is earned.
        progress = next
        val id = account?.id
        viewModelScope.launch { prefs.writeProgress(next, id) }
        if (id != null) queueSync(next)
    }

    fun resetStats() = updateProgress { it.scoreCleared() }

    /** Called by the reader when a chapter's last page comes into view. */
    fun markChapterRead(index: Int) {
        if (index in progress.chapters) return
        updateProgress { it.withChapterRead(index) }
    }

    // ---- account -------------------------------------------------------------

    fun selectAuthMode(value: AuthMode) {
        authError = null
        // Toggling between signing in and registering keeps whatever was last
        // said — "Signed out." is still true on both. Stepping into or out of
        // the reset form does not, because that message was about the form
        // being left behind.
        if (value == AuthMode.Forgot || authMode == AuthMode.Forgot) authNotice = null
        authMode = value
    }

    /** Abandons a half-finished sign-in and goes back to the email box. */
    fun cancelChallenge() {
        challenge = null
        authError = null
        authNotice = null
    }

    /**
     * Is the session still good?
     *
     * A refusal signs the app out locally; anything else — no signal, the server
     * down, a tunnel hiccup — leaves it exactly as it was. A phone is offline
     * far more often than a browser tab is, and being thrown out of an account
     * because a train went into a tunnel would be absurd.
     */
    private fun restoreSession() {
        if (sessionToken == null) return
        viewModelScope.launch {
            try {
                val live = api.me()
                if (live == null) {
                    prefs.clearSession()
                } else {
                    adopt(live.token, live.account, live.progress)
                }
            } catch (e: ApiException) {
                if (e.code != "network") prefs.clearSession()
            }
        }
    }

    /**
     * Step one: the password is checked and a code is emailed.
     *
     * Nothing is adopted here and no session exists yet, deliberately — a
     * stolen password on its own earns somebody a challenge token and an email
     * landing in the real owner's inbox.
     */
    fun beginSignIn(email: String, password: String, name: String) {
        if (authBusy) return
        val address = email.trim()
        // Checked here as well as on the server so the two common mistakes cost
        // no round trip and are answered in the reader's own language.
        if (!EMAIL.matches(address)) { authError = "email"; return }
        if (password.length < 8) { authError = "password"; return }

        val registering = authMode == AuthMode.Register
        authBusy = true
        authError = null
        authNotice = null
        viewModelScope.launch {
            try {
                val started = if (registering) {
                    api.register(address, password, name.trim().take(40), settings.lang.code)
                } else {
                    api.login(address, password, settings.lang.code)
                }
                challenge = started.token
                challengeEmail = started.email
            } catch (e: ApiException) {
                authError = e.code
            }
            authBusy = false
        }
    }

    /**
     * Step two: the emailed code, which is what creates the session.
     *
     * So this is also where whatever was earned signed out gets folded into the
     * account — and the guest copy is then deleted, which is what makes *adding*
     * the counters right rather than double-counting them.
     */
    fun verifyCode(code: String) {
        val token = challenge ?: return
        if (authBusy) return
        if (code.length < 6) { authError = "code"; return }

        authBusy = true
        authError = null
        authNotice = null
        viewModelScope.launch {
            try {
                val guest = progress
                val session = api.verify(token, code)
                var earned = session.progress
                // A failed merge keeps the guest copy: it is still theirs, and
                // the next sign-in will offer it again.
                val folded = guest.isEmpty ||
                    runCatching { earned = api.mergeProgress(guest) }.isSuccess
                // Adopted before the guest copy goes, so the numbers on screen
                // step straight from the old total to the new one instead of
                // through zero between two writes.
                adopt(session.token, session.account, earned)
                if (folded) prefs.clearGuestProgress()
                challenge = null
                authNotice = AuthNotice.SignedIn
            } catch (e: ApiException) {
                authError = e.code
                // An expired or burnt-out challenge can never be retried, so the
                // form goes back to the start rather than leaving a code box
                // that cannot work.
                if (e.code == "expired" || e.code == "attempts") challenge = null
            }
            authBusy = false
        }
    }

    fun resendCode() {
        val token = challenge ?: return
        if (authBusy) return
        authBusy = true
        authError = null
        authNotice = null
        viewModelScope.launch {
            try {
                val started = api.resend(token, challengeEmail, settings.lang.code)
                challenge = started.token
                authNotice = AuthNotice.CodeResent
            } catch (e: ApiException) {
                authError = e.code
                if (e.code == "expired") challenge = null
            }
            authBusy = false
        }
    }

    /**
     * Asking for a reset link.
     *
     * The link arrives by email and opens the website, because setting a
     * password is a thing you do once from wherever you happen to be reading
     * the email. The answer is the same whether or not the address has an
     * account, so this reports only that something is on its way if it does.
     */
    fun requestPasswordReset(email: String) {
        if (authBusy) return
        val address = email.trim()
        if (!EMAIL.matches(address)) { authError = "email"; return }

        authBusy = true
        authError = null
        authNotice = null
        viewModelScope.launch {
            try {
                api.requestReset(address, settings.lang.code)
                challengeEmail = address
                authNotice = AuthNotice.ResetSent
            } catch (e: ApiException) {
                authError = e.code
            }
            authBusy = false
        }
    }

    fun signOut() {
        if (authBusy) return
        authBusy = true
        viewModelScope.launch {
            // The token is dead to this device either way, so a failed call is
            // not a reason to stay signed in on the phone.
            runCatching { api.logout() }
            prefs.clearSession()
            authMode = AuthMode.SignIn
            authError = null
            authNotice = AuthNotice.SignedOut
            challenge = null
            authBusy = false
        }
    }

    private suspend fun adopt(token: String?, who: Account, remote: Progress) {
        // The copy on the device can be ahead of the server if the last sync
        // failed, so take whichever was written later rather than trusting the
        // wire blindly.
        val local = progress
        val localWins = account?.id == who.id && local.updatedAt > remote.updatedAt
        val keep = if (localWins) local else remote
        prefs.saveSession(token, who, keep)
        // Being ahead means a save never made it up — most likely the app was
        // killed while it was still inside the sync debounce. Push it now,
        // rather than leaving the account behind until the next exercise.
        if (localWins) queueSync(keep)
    }

    // ---- syncing -------------------------------------------------------------

    private var pendingSync: Progress? = null
    private var syncArmed = false
    private var syncJob: Job? = null

    /**
     * Debounced, because finishing an exercise writes progress once but paging
     * through a chapter writes it several times in a few seconds — and because
     * a save that fails must not take the next one down with it.
     */
    private fun queueSync(next: Progress) {
        pendingSync = next
        if (syncArmed) return
        syncArmed = true
        syncJob = viewModelScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            syncArmed = false
            val payload = pendingSync ?: return@launch
            pendingSync = null
            // Stays on the device if it fails; the next successful save carries
            // it up, because every save sends the whole blob.
            runCatching { api.saveProgress(payload) }
        }
    }

    /** The app going to the background mid-debounce would otherwise drop the
     *  last save of a session. */
    fun flushSync() {
        val payload = pendingSync ?: return
        if (account == null) return
        pendingSync = null
        syncArmed = false
        syncJob?.cancel()
        viewModelScope.launch { runCatching { api.saveProgress(payload) } }
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
            val kind = settings.kind
            updateProgress { it.withResult(kind, clean = mistakes == 0) }
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
