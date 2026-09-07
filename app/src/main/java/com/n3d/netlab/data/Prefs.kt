package com.n3d.netlab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

data class Settings(
    val lang: Lang,
    val theme: ThemeMode = ThemeMode.System,
    val kind: ExerciseKind = ExerciseKind.Vlsm,
    val difficulty: Difficulty = Difficulty.Easy,
    val solved: Int = 0,
    val streak: Int = 0,
    val best: Int = 0,
    /**
     * Everything the Progress card reports, kept apart from the three headline
     * numbers because [resetStats] clears those and leaves the reading alone —
     * chapters you have read are not a score.
     */
    val clean: Int = 0,
    val solvedVlsm: Int = 0,
    val solvedAnalyze: Int = 0,
    /** Indices of the chapters the reader has paged all the way through. */
    val chaptersRead: Set<Int> = emptySet(),
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

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            lang = p[KeyLang]?.let { name -> Lang.entries.firstOrNull { it.name == name } } ?: deviceLang(),
            theme = p[KeyTheme]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.System,
            kind = p[KeyKind]?.let { name -> ExerciseKind.entries.firstOrNull { it.name == name } } ?: ExerciseKind.Vlsm,
            difficulty = p[KeyDifficulty]?.let { name -> Difficulty.entries.firstOrNull { it.name == name } } ?: Difficulty.Easy,
            solved = p[KeySolved] ?: 0,
            streak = p[KeyStreak] ?: 0,
            best = p[KeyBest] ?: 0,
            clean = p[KeyClean] ?: 0,
            solvedVlsm = p[KeySolvedVlsm] ?: 0,
            solvedAnalyze = p[KeySolvedAnalyze] ?: 0,
            chaptersRead = p[KeyChapters].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
        )
    }

    suspend fun setLang(value: Lang) = context.dataStore.edit { it[KeyLang] = value.name }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[KeyTheme] = value.name }
    suspend fun setKind(value: ExerciseKind) = context.dataStore.edit { it[KeyKind] = value.name }
    suspend fun setDifficulty(value: Difficulty) = context.dataStore.edit { it[KeyDifficulty] = value.name }

    /**
     * One finished exercise. `clean` means no stage needed a second go.
     *
     * Every exercise that reaches the end counts as solved; only a clean one
     * extends the streak. Reading the walkthrough is help, and help is exactly
     * what the streak is there to measure the absence of.
     */
    suspend fun recordResult(kind: ExerciseKind, clean: Boolean) = context.dataStore.edit { p ->
        p[KeySolved] = (p[KeySolved] ?: 0) + 1
        when (kind) {
            ExerciseKind.Vlsm -> p[KeySolvedVlsm] = (p[KeySolvedVlsm] ?: 0) + 1
            ExerciseKind.Analyze -> p[KeySolvedAnalyze] = (p[KeySolvedAnalyze] ?: 0) + 1
        }
        if (clean) {
            val streak = (p[KeyStreak] ?: 0) + 1
            p[KeyClean] = (p[KeyClean] ?: 0) + 1
            p[KeyStreak] = streak
            p[KeyBest] = maxOf(p[KeyBest] ?: 0, streak)
        } else {
            p[KeyStreak] = 0
        }
    }

    /**
     * Reaching the last page is what counts as having read a chapter. It is the
     * only signal there is, and it is the honest one: the reader got there.
     */
    suspend fun markChapterRead(index: Int) = context.dataStore.edit { p ->
        p[KeyChapters] = p[KeyChapters].orEmpty() + index.toString()
    }

    /** Clears the score. The chapters read are reading, not score, and stay. */
    suspend fun resetStats() = context.dataStore.edit { p ->
        p[KeySolved] = 0
        p[KeyStreak] = 0
        p[KeyBest] = 0
        p[KeyClean] = 0
        p[KeySolvedVlsm] = 0
        p[KeySolvedAnalyze] = 0
    }

    private companion object {
        val KeyLang = stringPreferencesKey("lang")
        val KeyTheme = stringPreferencesKey("theme")
        val KeyKind = stringPreferencesKey("kind")
        val KeyDifficulty = stringPreferencesKey("difficulty")
        val KeySolved = intPreferencesKey("solved")
        val KeyStreak = intPreferencesKey("streak")
        val KeyBest = intPreferencesKey("best")
        val KeyClean = intPreferencesKey("clean")
        val KeySolvedVlsm = intPreferencesKey("solvedVlsm")
        val KeySolvedAnalyze = intPreferencesKey("solvedAnalyze")
        val KeyChapters = stringSetPreferencesKey("chaptersRead")
    }
}
