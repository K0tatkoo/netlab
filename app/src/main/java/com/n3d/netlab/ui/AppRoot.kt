package com.n3d.netlab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppMode
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.Sheet
import com.n3d.netlab.ui.components.NeuIconButton
import com.n3d.netlab.ui.screens.CalculatorScreen
import com.n3d.netlab.ui.screens.ExerciseScreen
import com.n3d.netlab.ui.screens.LearnScreen
import com.n3d.netlab.ui.screens.SettingsScreen
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.ui.theme.neuRaised
import kotlin.math.roundToInt

/**
 * Two modes, one switch.
 *
 * The app used to open on a four-tab bar — Practice, Calculator, Learn,
 * Settings — which asked a newcomer to choose between four things before it
 * had told them what any of them were. There are really only two: read the
 * method, or practise it. The calculator and the settings are tools, so they
 * are two small icons in the corner rather than half the navigation.
 */
@Composable
fun AppRoot(vm: AppViewModel) {
    val neu = LocalNeu.current
    val s = vm.strings

    BackHandler(enabled = vm.sheet != Sheet.None) { vm.closeSheet() }

    Column(
        Modifier
            .fillMaxSize()
            .background(neu.bg)
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (vm.sheet) {
                    Sheet.Calculator -> s.calculatorTitle
                    Sheet.Settings -> s.settingsTitle
                    Sheet.None -> s.appName
                },
                style = NeuType.Title,
                color = neu.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            NeuIconButton(
                Icons.Rounded.Calculate,
                s.calculatorTitle,
                onClick = {
                    if (vm.sheet == Sheet.Calculator) vm.closeSheet() else vm.openSheet(Sheet.Calculator)
                },
                tint = if (vm.sheet == Sheet.Calculator) neu.accent else null,
            )
            Spacer(Modifier.width(8.dp))
            NeuIconButton(
                Icons.Rounded.Tune,
                s.settingsTitle,
                onClick = {
                    if (vm.sheet == Sheet.Settings) vm.closeSheet() else vm.openSheet(Sheet.Settings)
                },
                tint = if (vm.sheet == Sheet.Settings) neu.accent else null,
            )
        }

        if (vm.sheet == Sheet.None) {
            ModeSwitch(
                mode = vm.mode,
                onSelect = vm::selectMode,
                learnLabel = s.tabLearn,
                exerciseLabel = s.tabExercise,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.sheet) {
                Sheet.Calculator -> CalculatorScreen(vm)
                Sheet.Settings -> SettingsScreen(vm)
                Sheet.None -> when (vm.mode) {
                    AppMode.Learn -> LearnScreen(vm)
                    AppMode.Exercise -> ExerciseScreen(vm)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The one control the whole app hangs off.
 *
 * Deliberately bigger and louder than the segmented controls inside the
 * screens: it is a mode switch, not a filter, and it has to read as the first
 * decision on the screen rather than as one more row of options.
 */
@Composable
internal fun ModeSwitch(
    mode: AppMode,
    onSelect: (AppMode) -> Unit,
    learnLabel: String,
    exerciseLabel: String,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeu.current
    val options = listOf(
        Triple(AppMode.Learn, learnLabel, Icons.AutoMirrored.Rounded.MenuBook),
        Triple(AppMode.Exercise, exerciseLabel, Icons.Rounded.Edit),
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(62.dp)
            .neuInset(radius = NeuRadius.Pill, depth = NeuDepths.Inset)
            .padding(5.dp),
    ) {
        val itemWidth = maxWidth / options.size
        val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
        val index = options.indexOfFirst { it.first == mode }.coerceAtLeast(0)
        // Animated in pixels and applied through the lambda overload of offset:
        // the Dp overload would recompose this subtree on every frame of the
        // slide, where the lambda only re-runs layout.
        val offsetPx by animateFloatAsState(
            targetValue = itemWidthPx * index,
            animationSpec = tween(NeuMotion.SlideMs, easing = NeuMotion.Ease),
            label = "mode",
        )
        Box(
            Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .neuRaised(radius = NeuRadius.Pill, depth = NeuDepths.Sm),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { (value, label, icon) ->
                ModeItem(
                    label = label,
                    icon = icon,
                    active = value == mode,
                    modifier = Modifier.weight(1f),
                ) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun ModeItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val neu = LocalNeu.current
    val tint by animateColorAsState(
        if (active) neu.accent else neu.faint,
        tween(NeuMotion.FadeMs),
        label = "modeTint",
    )
    Row(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            style = NeuType.Label.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
            ),
            color = tint,
            maxLines = 1,
        )
    }
}
