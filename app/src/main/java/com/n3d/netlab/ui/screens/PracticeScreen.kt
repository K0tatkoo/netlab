package com.n3d.netlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.core.Allocation
import com.n3d.netlab.core.AnalyzeField
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.core.FieldState
import com.n3d.netlab.core.Grader
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.VlsmField
import com.n3d.netlab.core.steps
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.render
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.Dot
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuIconButton
import com.n3d.netlab.ui.components.NeuSegmented
import com.n3d.netlab.ui.components.NeuTextField
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.components.segmentPalette
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

@Composable
fun PracticeScreen(vm: AppViewModel) {
    val s = vm.strings
    val neu = LocalNeu.current

    if (vm.solutionOpen) {
        // Recomputed only when the task or the language actually changes —
        // rendering eleven steps of prose on every recomposition of a screen
        // that recomposes on every keystroke would be noticeable.
        val cards = remember(vm.settings.kind, vm.vlsmTask, vm.analyzeTask, s) {
            when (vm.settings.kind) {
                ExerciseKind.Vlsm -> vm.vlsmTask.plan.steps().map { render(it, s) }
                ExerciseKind.Analyze -> vm.analyzeTask.steps().map { render(it, s) }
            }
        }
        StepsSheet(s.solutionTitle, cards, s, onClose = vm::closeSolution)
        return
    }

    val palette = segmentPalette(neu)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.practiceTitle, style = NeuType.Title, color = neu.text, modifier = Modifier.weight(1f))
                Stat(s.labelSolved, vm.settings.solved.toString())
                Spacer(Modifier.width(14.dp))
                Stat(s.labelStreak, vm.settings.streak.toString(), neu.accent)
                Spacer(Modifier.width(14.dp))
                Stat(s.labelBest, vm.settings.best.toString())
            }
        }

        item {
            NeuSegmented(
                options = ExerciseKind.entries,
                selected = vm.settings.kind,
                onSelect = vm::setKind,
                label = { if (it == ExerciseKind.Vlsm) s.kindVlsm else s.kindAnalyze },
            )
        }

        item {
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

        item {
            when (vm.settings.kind) {
                ExerciseKind.Vlsm -> VlsmAssignment(vm, s, palette)
                ExerciseKind.Analyze -> AnalyzeAssignment(vm, s)
            }
        }

        when (vm.settings.kind) {
            ExerciseKind.Vlsm -> {
                items(vm.vlsmTask.plan.inTaskOrder, key = { it.requirement.name }) { alloc ->
                    SubnetAnswerCard(vm, s, alloc, palette[alloc.originalIndex % palette.size])
                }
            }
            ExerciseKind.Analyze -> {
                item { AnalyzeAnswerCard(vm, s) }
            }
        }

        if (vm.checked) {
            item {
                val tone = when {
                    vm.allCorrect -> neu.ok
                    vm.wrongCount > 0 -> neu.err
                    else -> neu.warn
                }
                val text = when {
                    vm.isBlank -> s.verdictNothing
                    vm.allCorrect -> s.verdictPerfect
                    vm.wrongCount > 0 -> s.verdictWrong(vm.wrongCount, vm.askedCount)
                    else -> s.verdictIncomplete
                }
                Banner(text, tone)
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeuButton(
                        text = s.actionCheck,
                        onClick = vm::check,
                        icon = Icons.Rounded.Done,
                        tone = ButtonTone.Accent,
                        modifier = Modifier.weight(1f),
                        fill = true,
                    )
                    NeuButton(
                        text = s.actionSolution,
                        onClick = vm::openSolution,
                        icon = Icons.Rounded.School,
                        modifier = Modifier.weight(1f),
                        fill = true,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeuButton(
                        text = s.actionNewExercise,
                        onClick = vm::newExercise,
                        icon = Icons.Rounded.Refresh,
                        modifier = Modifier.weight(1f),
                        fill = true,
                    )
                    NeuIconButton(
                        Icons.Rounded.AutoFixHigh,
                        s.actionFillCorrect,
                        vm::fillCorrect,
                        size = 46.dp,
                    )
                    NeuIconButton(
                        Icons.AutoMirrored.Rounded.Backspace,
                        s.actionClear,
                        vm::clearAnswers,
                        size = 46.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, tone: Color? = null) {
    val neu = LocalNeu.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NeuType.MetricSm, color = tone ?: neu.text, maxLines = 1)
        Text(label.uppercase(), style = NeuType.Small.copy(fontSize = 9.5.sp), color = neu.faint, maxLines = 1)
    }
}

@Composable
private fun VlsmAssignment(vm: AppViewModel, s: Strings, palette: List<Color>) {
    val neu = LocalNeu.current
    val task = vm.vlsmTask
    NeuCard {
        SectionLabel(s.assignment)
        Text(s.baseNetwork, style = NeuType.Label, color = neu.faint)
        Spacer(Modifier.height(4.dp))
        Text(task.label, style = NeuType.Metric, color = neu.accent, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Text(s.vlsmPrompt, style = NeuType.Small.copy(lineHeight = 17.sp), color = neu.dim)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column {
                task.requirements.forEachIndexed { index, req ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(palette[index % palette.size], 9.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            req.name,
                            style = NeuType.Label.copy(fontWeight = FontWeight.Bold),
                            color = neu.text,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(
                            s.hosts(req.hosts.toLong()),
                            style = NeuType.Mono,
                            color = neu.dim,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzeAssignment(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val task = vm.analyzeTask
    NeuCard {
        SectionLabel(s.assignment)
        Text(s.givenAddress, style = NeuType.Label, color = neu.faint)
        Spacer(Modifier.height(4.dp))
        Text(
            Ip.cidr(task.address, task.prefix),
            style = NeuType.Metric,
            color = neu.accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Text(s.analyzePrompt, style = NeuType.Small.copy(lineHeight = 17.sp), color = neu.dim)
    }
}

@Composable
private fun SubnetAnswerCard(vm: AppViewModel, s: Strings, alloc: Allocation, tone: Color) {
    val neu = LocalNeu.current
    val name = alloc.requirement.name
    val answer = vm.answerFor(name)
    val grades = if (vm.checked) vm.vlsmGrades[name].orEmpty() else emptyMap()
    val fields = vm.vlsmFields

    NeuCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(tone, 10.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                s.subnetTitle(name),
                style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = neu.text,
                modifier = Modifier.weight(1f),
            )
            Text(s.hosts(alloc.requirement.hosts.toLong()), style = NeuType.Mono, color = neu.faint)
        }
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnswerField(
                label = s.fieldNetwork,
                value = answer.network,
                onValueChange = { vm.updateVlsm(name, VlsmField.Network, it) },
                state = grades[VlsmField.Network],
                expected = Grader.expected(VlsmField.Network, alloc),
                s = s,
                modifier = Modifier.weight(1f),
            )
            if (VlsmField.Prefix in fields) {
                AnswerField(
                    label = s.fieldPrefix,
                    value = answer.prefix,
                    onValueChange = { vm.updateVlsm(name, VlsmField.Prefix, it) },
                    state = grades[VlsmField.Prefix],
                    expected = Grader.expected(VlsmField.Prefix, alloc),
                    s = s,
                    placeholder = "/26",
                    modifier = Modifier.width(86.dp),
                )
            }
        }

        if (VlsmField.FirstHost in fields) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnswerField(
                    label = s.fieldFirstHost,
                    value = answer.firstHost,
                    onValueChange = { vm.updateVlsm(name, VlsmField.FirstHost, it) },
                    state = grades[VlsmField.FirstHost],
                    expected = Grader.expected(VlsmField.FirstHost, alloc),
                    s = s,
                    modifier = Modifier.weight(1f),
                )
                AnswerField(
                    label = s.fieldLastHost,
                    value = answer.lastHost,
                    onValueChange = { vm.updateVlsm(name, VlsmField.LastHost, it) },
                    state = grades[VlsmField.LastHost],
                    expected = Grader.expected(VlsmField.LastHost, alloc),
                    s = s,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            AnswerField(
                label = s.fieldBroadcast,
                value = answer.broadcast,
                onValueChange = { vm.updateVlsm(name, VlsmField.Broadcast, it) },
                state = grades[VlsmField.Broadcast],
                expected = Grader.expected(VlsmField.Broadcast, alloc),
                s = s,
            )
        }
    }
}

@Composable
private fun AnalyzeAnswerCard(vm: AppViewModel, s: Strings) {
    val task = vm.analyzeTask
    val answer = vm.analyzeAnswer
    val grades = if (vm.checked) vm.analyzeGrades else emptyMap()
    val fields = vm.analyzeFields

    NeuCard {
        SectionLabel(s.yourAnswer)
        fields.forEachIndexed { index, field ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            AnswerField(
                label = when (field) {
                    AnalyzeField.Network -> s.fieldNetwork
                    AnalyzeField.Mask -> s.fieldMask
                    AnalyzeField.FirstHost -> s.fieldFirstHost
                    AnalyzeField.LastHost -> s.fieldLastHost
                    AnalyzeField.Broadcast -> s.fieldBroadcast
                    AnalyzeField.Hosts -> s.fieldUsableHosts
                },
                value = answer[field],
                onValueChange = { vm.updateAnalyze(field, it) },
                state = grades[field],
                expected = Grader.expected(field, task),
                s = s,
                numeric = field == AnalyzeField.Hosts,
            )
        }
    }
}

/**
 * One answer box.
 *
 * The verdict is carried by the label and the text colour rather than by a
 * border, because a border is the one thing this design system does not have.
 * A wrong field also prints the right answer underneath: hiding it would only
 * send the learner to the walkthrough for a number they were one digit away
 * from.
 */
@Composable
private fun AnswerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    state: FieldState?,
    expected: String,
    s: Strings,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
) {
    val neu = LocalNeu.current
    val tone = when (state) {
        FieldState.Correct -> neu.ok
        FieldState.Wrong -> neu.err
        else -> null
    }
    Column(modifier) {
        NeuTextField(
            value = value,
            onValueChange = { raw ->
                // Czech keypads offer a comma where the decimal pad expects a
                // dot; typing 10,0,0,0 into an IP field is otherwise a dead end.
                val cleaned = raw.replace(',', '.')
                    .filter { it.isDigit() || it == '.' || it == '/' }
                onValueChange(cleaned)
            },
            label = label,
            placeholder = placeholder,
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Decimal,
            textStyle = NeuType.Mono.copy(fontSize = 14.sp),
            accent = tone,
            minHeight = 48.dp,
        )
        if (state == FieldState.Wrong) {
            Text(
                "${s.labelExpected}: $expected",
                style = NeuType.Small,
                color = neu.faint,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                maxLines = 1,
            )
        }
    }
}
