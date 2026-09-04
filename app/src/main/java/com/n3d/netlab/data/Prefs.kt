package com.n3d.netlab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    /** Ask only for the network address and prefix, not the whole range. */
    val shortFields: Boolean = false,
    val solved: Int = 0,
    val streak: Int = 0,
    val best: Int = 0,
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
            shortFields = p[KeyShortFields] ?: false,
            solved = p[KeySolved] ?: 0,
            streak = p[KeyStreak] ?: 0,
            best = p[KeyBest] ?: 0,
        )
    }

    suspend fun setLang(value: Lang) = context.dataStore.edit { it[KeyLang] = value.name }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[KeyTheme] = value.name }
    suspend fun setKind(value: ExerciseKind) = context.dataStore.edit { it[KeyKind] = value.name }
    suspend fun setDifficulty(value: Difficulty) = context.dataStore.edit { it[KeyDifficulty] = value.name }
    suspend fun setShortFields(value: Boolean) = context.dataStore.edit { it[KeyShortFields] = value }

    suspend fun recordResult(correct: Boolean) = context.dataStore.edit { p ->
        if (correct) {
            val streak = (p[KeyStreak] ?: 0) + 1
            p[KeySolved] = (p[KeySolved] ?: 0) + 1
            p[KeyStreak] = streak
            p[KeyBest] = maxOf(p[KeyBest] ?: 0, streak)
        } else {
            p[KeyStreak] = 0
        }
    }

    suspend fun resetStats() = context.dataStore.edit { p ->
        p[KeySolved] = 0
        p[KeyStreak] = 0
        p[KeyBest] = 0
    }

    private companion object {
        val KeyLang = stringPreferencesKey("lang")
        val KeyTheme = stringPreferencesKey("theme")
        val KeyKind = stringPreferencesKey("kind")
        val KeyDifficulty = stringPreferencesKey("difficulty")
        val KeyShortFields = booleanPreferencesKey("short_fields")
        val KeySolved = intPreferencesKey("solved")
        val KeyStreak = intPreferencesKey("streak")
        val KeyBest = intPreferencesKey("best")
    }
}
