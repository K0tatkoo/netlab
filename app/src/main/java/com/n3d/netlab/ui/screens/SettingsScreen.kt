package com.n3d.netlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.data.ThemeMode
import com.n3d.netlab.i18n.Lang
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuSegmented
import com.n3d.netlab.ui.components.Overlay
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = vm.strings
    val neu = LocalNeu.current
    var confirmReset by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                NeuCard {
                    SectionLabel(s.settingLanguage)
                    NeuSegmented(
                        options = Lang.entries,
                        selected = vm.settings.lang,
                        onSelect = vm::setLang,
                        label = { if (it == Lang.En) s.languageEnglish else s.languageCzech },
                    )
                    Spacer(Modifier.height(18.dp))
                    SectionLabel(s.settingTheme)
                    NeuSegmented(
                        options = ThemeMode.entries,
                        selected = vm.settings.theme,
                        onSelect = vm::setTheme,
                        label = {
                            when (it) {
                                ThemeMode.System -> s.themeSystem
                                ThemeMode.Light -> s.themeLight
                                ThemeMode.Dark -> s.themeDark
                            }
                        },
                    )
                }
            }

            item {
                NeuCard {
                    SectionLabel(s.settingDefaults)
                    Text(s.settingDefaultKind, style = NeuType.Label, color = neu.dim)
                    Spacer(Modifier.height(8.dp))
                    NeuSegmented(
                        options = ExerciseKind.entries,
                        selected = vm.settings.kind,
                        onSelect = vm::setKind,
                        label = { if (it == ExerciseKind.Vlsm) s.kindVlsm else s.kindAnalyze },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(s.settingDefaultDifficulty, style = NeuType.Label, color = neu.dim)
                    Spacer(Modifier.height(8.dp))
                    NeuSegmented(
                        options = Difficulty.entries,
                        selected = vm.settings.difficulty,
                        onSelect = vm::setDifficulty,
                        label = {
                            when (it) {
                                Difficulty.Easy -> s.diffEasy
                                Difficulty.Medium -> s.diffMedium
                                Difficulty.Hard -> s.diffHard
                            }
                        },
                    )
                }
            }

            item {
                NeuCard {
                    SectionLabel(s.settingStats)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                            .padding(vertical = 16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            BigStat(s.labelSolved, vm.settings.solved.toString())
                            BigStat(s.labelStreak, vm.settings.streak.toString(), accent = true)
                            BigStat(s.labelBest, vm.settings.best.toString())
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    NeuButton(
                        text = s.settingResetStats,
                        onClick = { confirmReset = true },
                        icon = Icons.Rounded.DeleteSweep,
                        tone = ButtonTone.Danger,
                        fill = true,
                    )
                }
            }

            item {
                NeuCard {
                    SectionLabel(s.settingAbout)
                    Text(
                        s.aboutBody,
                        style = NeuType.Small.copy(lineHeight = 18.sp),
                        color = neu.dim,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("NetLab 1.0", style = NeuType.Mono, color = neu.faint)
                }
            }
        }

        if (confirmReset) {
            Overlay(onDismiss = { confirmReset = false }) {
                Text(
                    s.settingResetStats,
                    style = NeuType.Title.copy(fontSize = 18.sp),
                    color = neu.text,
                )
                Spacer(Modifier.height(8.dp))
                Text(s.settingResetStatsHint, style = NeuType.Small, color = neu.dim)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeuButton(
                        text = s.actionCancel,
                        onClick = { confirmReset = false },
                        modifier = Modifier.weight(1f),
                        fill = true,
                    )
                    NeuButton(
                        text = s.actionConfirm,
                        onClick = {
                            vm.resetStats()
                            confirmReset = false
                        },
                        tone = ButtonTone.Danger,
                        modifier = Modifier.weight(1f),
                        fill = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, accent: Boolean = false) {
    val neu = LocalNeu.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NeuType.Metric, color = if (accent) neu.accent else neu.text)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), style = NeuType.Section, color = neu.faint)
    }
}
