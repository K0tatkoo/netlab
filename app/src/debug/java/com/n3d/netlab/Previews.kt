package com.n3d.netlab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.data.ThemeMode
import com.n3d.netlab.i18n.Cs
import com.n3d.netlab.i18n.En
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.ModeSwitch
import com.n3d.netlab.ui.components.LessonBlock
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.segmentPalette
import com.n3d.netlab.ui.screens.ChapterList
import com.n3d.netlab.ui.screens.ChapterReader
import com.n3d.netlab.ui.screens.SubnetOrderList
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NetLabTheme

/**
 * Design previews, for the preview pane in Android Studio.
 *
 * Debug-only, so nothing here ships. They cover the parts of the redesign that
 * are new — the mode switch, the course pages with their worked-calculation
 * blocks, and the drag-to-sort stage — rather than the screens that were only
 * rearranged.
 *
 * They are not rendered on this machine: `com.android.compose.screenshot`
 * discovers zero previews under AGP 8.13 (it never puts its JUnit engine on the
 * screenshotTest runtime classpath, and adding it by hand breaks the built-in
 * Kotlin compile), so the layoutlib route that works on the Velocity project is
 * not available here. Open this file in Android Studio to see them.
 */
@Composable
private fun Frame(dark: Boolean = false, content: @Composable () -> Unit) {
    NetLabTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) {
        val neu = LocalNeu.current
        Column(
            Modifier
                .fillMaxSize()
                .background(neu.bg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
    }
}

// ---- the one control the app hangs off -------------------------------------

@Preview(name = "Mode switch", device = "spec:width=411dp,height=220dp")
@Composable
fun ModeSwitchPreview() {
    Frame {
        ModeSwitch(AppMode.Learn, {}, En.tabLearn, En.tabExercise)
        ModeSwitch(AppMode.Exercise, {}, En.tabLearn, En.tabExercise)
    }
}

@Preview(name = "Mode switch dark", device = "spec:width=411dp,height=220dp")
@Composable
fun ModeSwitchDarkPreview() {
    Frame(dark = true) {
        ModeSwitch(AppMode.Learn, {}, Cs.tabLearn, Cs.tabExercise)
        ModeSwitch(AppMode.Exercise, {}, Cs.tabLearn, Cs.tabExercise)
    }
}

// ---- Learn -----------------------------------------------------------------

@Preview(name = "Chapter list", device = "spec:width=411dp,height=915dp")
@Composable
fun ChapterListPreview() {
    NetLabTheme(ThemeMode.Light) {
        val neu = LocalNeu.current
        Column(Modifier.fillMaxSize().background(neu.bg)) {
            ChapterList(En, read = setOf(0, 1), onOpen = {})
        }
    }
}

/** Chapter 5's worked example — the densest page in the course. */
@Preview(name = "Worked example page", device = "spec:width=411dp,height=1600dp")
@Composable
fun WorkedExamplePreview() {
    ChapterPage(En, chapter = 4, page = 1)
}

/** The VLSM chapter's "why largest first" page, which carries the split bar. */
@Preview(name = "Why sort first", device = "spec:width=411dp,height=1400dp")
@Composable
fun SortingPagePreview() {
    ChapterPage(En, chapter = 6, page = 2)
}

/** Binary, the place-value strip and a check question, in Czech. */
@Preview(name = "Binary page (cs)", device = "spec:width=411dp,height=1600dp")
@Composable
private fun BinaryPageCzechPreview() {
    ChapterPage(Cs, chapter = 1, page = 1, dark = true)
}

@Preview(name = "Chapter reader", device = "spec:width=411dp,height=915dp")
@Composable
fun ChapterReaderPreview() {
    NetLabTheme(ThemeMode.Light) {
        ChapterReader(
            chapter = En.course[0],
            number = 1,
            total = En.course.size,
            s = En,
            onClose = {},
            onNextChapter = {},
        )
    }
}

@Composable
private fun ChapterPage(s: Strings, chapter: Int, page: Int, dark: Boolean = false) {
    NetLabTheme(if (dark) ThemeMode.Dark else ThemeMode.Light) {
        val neu = LocalNeu.current
        Column(
            Modifier
                .fillMaxSize()
                .background(neu.bg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            NeuCard(padding = 18.dp) {
                s.course[chapter].pages[page].blocks.forEachIndexed { index, block ->
                    if (index > 0) androidx.compose.foundation.layout.Spacer(Modifier.padding(7.dp))
                    LessonBlock(block, s, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---- the drag-to-sort stage ------------------------------------------------

@Preview(name = "Sort stage", device = "spec:width=411dp,height=440dp")
@Composable
fun OrderStagePreview() {
    Frame {
        val palette = segmentPalette(LocalNeu.current)
        val requirements = listOf(
            Requirement("C", 14),
            Requirement("A", 60),
            Requirement("D", 7),
            Requirement("B", 30),
        )
        var order by remember { mutableStateOf(requirements) }
        SubnetOrderList(
            requirements = order,
            toneFor = { name -> palette[requirements.indexOfFirst { it.name == name } % palette.size] },
            locked = false,
            onMove = { from, to ->
                order = order.toMutableList().also { it.add(to, it.removeAt(from)) }
            },
            s = En,
        )
    }
}

@Preview(name = "Sort stage (cs, dark)", device = "spec:width=411dp,height=440dp")
@Composable
private fun OrderStageCzechPreview() {
    Frame(dark = true) {
        val palette = segmentPalette(LocalNeu.current)
        val requirements = listOf(
            Requirement("A", 500),
            Requirement("B", 200),
            Requirement("C", 60),
            Requirement("D", 2),
        )
        SubnetOrderList(
            requirements = requirements,
            toneFor = { name -> palette[requirements.indexOfFirst { it.name == name } % palette.size] },
            locked = false,
            onMove = { _, _ -> },
            s = Cs,
        )
    }
}
