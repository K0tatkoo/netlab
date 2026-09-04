package com.n3d.netlab.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.ui.screens.CalculatorScreen
import com.n3d.netlab.ui.screens.LearnScreen
import com.n3d.netlab.ui.screens.PracticeScreen
import com.n3d.netlab.ui.screens.SettingsScreen
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.ui.theme.neuRaised

private enum class Tab { Practice, Calculator, Learn, Settings }

@Composable
fun AppRoot(vm: AppViewModel) {
    val neu = LocalNeu.current
    val s = vm.strings
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex.coerceIn(0, Tab.entries.lastIndex)]

    Column(
        Modifier
            .fillMaxSize()
            .background(neu.bg)
            .imePadding(),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            when (tab) {
                Tab.Practice -> PracticeScreen(vm)
                Tab.Calculator -> CalculatorScreen(vm)
                Tab.Learn -> LearnScreen(vm)
                Tab.Settings -> SettingsScreen(vm)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .neuRaised(radius = NeuRadius.Lg, depth = NeuDepths.Sm)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.School, s.tabPractice, tab == Tab.Practice) { tabIndex = 0 }
            NavItem(Icons.Rounded.Calculate, s.tabCalculator, tab == Tab.Calculator) { tabIndex = 1 }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, s.tabLearn, tab == Tab.Learn) { tabIndex = 2 }
            NavItem(Icons.Rounded.Tune, s.tabSettings, tab == Tab.Settings) { tabIndex = 3 }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val neu = LocalNeu.current
    val tint by animateColorAsState(
        if (active) neu.accent else neu.faint,
        tween(NeuMotion.FadeMs),
        label = "navTint",
    )
    Column(
        Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 46.dp, height = 30.dp)
                // The active tab is a well, not a highlight: pressed-in is the
                // only "selected" state this design system has.
                .then(
                    if (active) Modifier.neuInset(NeuRadius.Pill, NeuDepths.InsetSm)
                    else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = NeuType.Small.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.5.sp,
            ),
            color = tint,
            maxLines = 1,
        )
    }
}
