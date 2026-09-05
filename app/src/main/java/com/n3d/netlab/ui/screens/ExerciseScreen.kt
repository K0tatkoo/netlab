package com.n3d.netlab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.core.AnalyzeField
import com.n3d.netlab.core.AnalyzeStage
import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.ExerciseKind
import com.n3d.netlab.core.FieldState
import com.n3d.netlab.core.Grader
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.core.VlsmField
import com.n3d.netlab.core.VlsmStage
import com.n3d.netlab.core.fields
import com.n3d.netlab.core.steps
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.render
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.Dot
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.InfoRow
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuSegmented
import com.n3d.netlab.ui.components.NeuTextField
import com.n3d.netlab.ui.components.PlanBar
import com.n3d.netlab.ui.components.PlanSegment
import com.n3d.netlab.ui.components.ReorderableColumn
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.components.segmentPalette
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.ui.theme.neuRaised

/**
 * One exercise, walked one stage at a time.
 *
 * The screen only ever asks for the one thing the current stage is about. That
 * is the whole redesign: the previous version put twenty empty boxes in front
 * of somebody who did not yet know what a mask was, and being told afterwards
 * that nine of them were wrong taught nothing, because every one of the nine
 * was downstream of the same first mistake.
 */
@Composable
fun ExerciseScreen(vm: AppViewModel) {
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

    var hintOpen by remember(vm.vlsmStage, vm.analyzeStage, vm.settings.kind) { mutableStateOf(false) }
    val palette = segmentPalette(neu)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ExerciseHeader(vm, s) }

        if (vm.stageDone) {
            item { ResultCard(vm, s, palette) }
            item { DoneActions(vm, s) }
            return@LazyColumn
        }

        item { StageHeader(vm, s) }
        item { AssignmentCard(vm, s, palette) }

        item {
            AnimatedVisibility(
                visible = hintOpen,
                enter = expandVertically(tween(NeuMotion.SlideMs, easing = NeuMotion.Ease)) + fadeIn(tween(NeuMotion.FadeMs)),
                exit = shrinkVertically(tween(NeuMotion.FadeMs)) + fadeOut(tween(120)),
            ) {
                HintCard(vm, s)
            }
        }

        item { StageBody(vm, s, palette) }

        item { Verdict(vm, s) }

        item { StageActions(vm, s, hintOpen) { hintOpen = !hintOpen } }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseHeader(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NeuSegmented(
            options = ExerciseKind.entries,
            selected = vm.settings.kind,
            onSelect = vm::setKind,
            label = { if (it == ExerciseKind.Vlsm) s.kindVlsm else s.kindAnalyze },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (vm.settings.kind == ExerciseKind.Vlsm) s.kindVlsmHint else s.kindAnalyzeHint,
                style = NeuType.Small,
                color = neu.faint,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${s.labelStreak.uppercase()} ${vm.settings.streak}",
                style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                color = neu.accent,
                maxLines = 1,
            )
        }
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

@Composable
private fun StageHeader(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val title = when (vm.settings.kind) {
        ExerciseKind.Vlsm -> s.vlsmStageTitle(vm.vlsmStage)
        ExerciseKind.Analyze -> s.analyzeStageTitle(vm.analyzeStage)
    }
    val prompt = when (vm.settings.kind) {
        ExerciseKind.Vlsm -> s.vlsmStagePrompt(vm.vlsmStage)
        ExerciseKind.Analyze -> s.analyzeStagePrompt(vm.analyzeStage)
    }
    Column(Modifier.fillMaxWidth()) {
        StageProgress(vm.stageTotal, vm.stageNumber - 1)
        Spacer(Modifier.height(10.dp))
        Text(
            s.stageOf(vm.stageNumber, vm.stageTotal).uppercase(),
            style = NeuType.Section,
            color = neu.accent,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            style = NeuType.Title,
            color = neu.text,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            prompt,
            style = NeuType.Small.copy(lineHeight = 18.sp),
            color = neu.dim,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun StageProgress(total: Int, current: Int) {
    val neu = LocalNeu.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(total) { i ->
            val alpha by animateFloatAsState(if (i <= current) 1f else 0.22f, tween(220), label = "stage")
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(NeuRadius.Pill))
                    .background(neu.accent.copy(alpha = alpha)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Assignment
// ---------------------------------------------------------------------------

@Composable
private fun AssignmentCard(vm: AppViewModel, s: Strings, palette: List<Color>) {
    val neu = LocalNeu.current
    NeuCard {
        SectionLabel(s.assignment)
        when (vm.settings.kind) {
            ExerciseKind.Vlsm -> {
                val task = vm.vlsmTask
                Text(s.baseNetwork, style = NeuType.Label, color = neu.faint)
                Spacer(Modifier.height(4.dp))
                Text(task.label, style = NeuType.Metric, color = neu.accent, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Column {
                        // Always the order the assignment was written in, even
                        // once the learner has dragged their own order above:
                        // this card is the question, not their working.
                        task.requirements.forEachIndexed { index, req ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Dot(palette[index % palette.size], 9.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    req.name,
                                    style = NeuType.Label.copy(fontWeight = FontWeight.Bold),
                                    color = neu.text,
                                    modifier = Modifier.width(26.dp),
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

            ExerciseKind.Analyze -> {
                val task = vm.analyzeTask
                Text(s.givenAddress, style = NeuType.Label, color = neu.faint)
                Spacer(Modifier.height(4.dp))
                Text(
                    Ip.cidr(task.address, task.prefix),
                    style = NeuType.Metric,
                    color = neu.accent,
                    maxLines = 1,
                )
                // Once a stage is answered its result stays on screen, so the
                // next stage is worked from the numbers rather than from memory.
                val known = mutableListOf<Pair<String, String>>()
                if (vm.analyzeStage.ordinal > AnalyzeStage.Mask.ordinal) {
                    known += s.fieldMask to Ip.format(task.mask)
                    known += s.fieldBlockSize to s.num(task.blockSize)
                }
                if (vm.analyzeStage.ordinal > AnalyzeStage.Network.ordinal) {
                    known += s.fieldNetwork to Ip.format(task.network)
                }
                if (vm.analyzeStage.ordinal > AnalyzeStage.Broadcast.ordinal) {
                    known += s.fieldBroadcast to Ip.format(task.broadcast)
                }
                if (known.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Column {
                            known.forEach { (label, value) ->
                                InfoRow(label, value)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// The stage itself
// ---------------------------------------------------------------------------

@Composable
private fun StageBody(vm: AppViewModel, s: Strings, palette: List<Color>) {
    when (vm.settings.kind) {
        ExerciseKind.Vlsm -> when (vm.vlsmStage) {
            VlsmStage.Order -> OrderStage(vm, s, palette)
            VlsmStage.Size -> SizeStage(vm, s, palette)
            VlsmStage.Place -> PlaceStage(vm, s, palette)
            VlsmStage.Done -> Unit
        }
        ExerciseKind.Analyze -> AnalyzeStageBody(vm, s)
    }
}

/**
 * Stage one of a VLSM exercise: drag the subnets into largest-first order.
 *
 * Sorting is a real step of the method and the one people skip, so it is asked
 * for as a move rather than as a typed answer — you cannot half-do a drag, and
 * the picture of the order is exactly what has to end up in the learner's head.
 */
@Composable
private fun OrderStage(vm: AppViewModel, s: Strings, palette: List<Color>) {
    SubnetOrderList(
        requirements = vm.orderDraft,
        // Colours follow the assignment card's order, so a subnet keeps its own
        // colour wherever it is dragged to.
        toneFor = { name ->
            palette[vm.vlsmTask.requirements.indexOfFirst { it.name == name }
                .coerceAtLeast(0) % palette.size]
        },
        locked = vm.stagePassed,
        onMove = vm::moveOrder,
        s = s,
    )
}

/**
 * The drag list itself, with no view model in sight so it can be rendered by
 * layoutlib. There is no emulator on this machine, so anything that cannot be
 * put in front of a preview cannot be looked at before it ships.
 */
@Composable
internal fun SubnetOrderList(
    requirements: List<Requirement>,
    toneFor: (String) -> Color,
    locked: Boolean,
    onMove: (Int, Int) -> Unit,
    s: Strings,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeu.current
    NeuCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(s.orderTopLabel.uppercase(), style = NeuType.Section, color = neu.faint)
            Text(s.orderPrompt, style = NeuType.Small, color = neu.accent)
        }

        ReorderableColumn(
            items = requirements,
            itemHeight = 54.dp,
            spacing = 9.dp,
            onMove = onMove,
            key = { it.name },
            enabled = !locked,
        ) { req, index, dragging, handle ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .neuRaised(
                        radius = NeuRadius.Md,
                        depth = if (dragging) NeuDepths.Lg else NeuDepths.Sm,
                    )
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}",
                    style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                    color = neu.faint,
                    modifier = Modifier.width(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Dot(toneFor(req.name), 10.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    req.name,
                    style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = neu.text,
                    modifier = Modifier.width(26.dp),
                )
                Text(
                    s.hosts(req.hosts.toLong()),
                    style = NeuType.Mono,
                    color = neu.dim,
                    modifier = Modifier.weight(1f),
                )
                // The handle's touch target is the whole 44dp box, not the
                // 22dp glyph: a drag that only starts on the icon itself is a
                // coin-toss with a thumb.
                Box(
                    Modifier.size(44.dp).then(handle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = s.dragHandle,
                        tint = if (locked) neu.faint.copy(alpha = 0.4f) else neu.faint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            s.orderBottomLabel.uppercase(),
            style = NeuType.Section,
            color = neu.faint,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/** Stage two: one prefix per subnet, in the order the learner just produced. */
@Composable
private fun SizeStage(vm: AppViewModel, s: Strings, palette: List<Color>) {
    val neu = LocalNeu.current
    val names = vm.vlsmTask.requirements.map { it.name }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        vm.orderedAllocations.forEach { alloc ->
            val name = alloc.requirement.name
            val tone = palette[names.indexOf(name).coerceAtLeast(0) % palette.size]
            NeuCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(tone, 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.subnetTitle(name),
                            style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = neu.text,
                        )
                        Text(
                            "${s.hosts(alloc.requirement.hosts.toLong())}  →  ${alloc.requirement.hosts} + 2 = ${alloc.requirement.hosts + 2}",
                            style = NeuType.Mono,
                            color = neu.faint,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    AnswerField(
                        label = s.fieldPrefix,
                        value = vm.answerFor(name).prefix,
                        onValueChange = { vm.updateVlsm(name, VlsmField.Prefix, it) },
                        state = if (vm.stageChecked) vm.vlsmGrades[name]?.get(VlsmField.Prefix) else null,
                        expected = Grader.expected(VlsmField.Prefix, alloc),
                        s = s,
                        placeholder = "/26",
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

/** Stage three: place every block and read its four landmark addresses off. */
@Composable
private fun PlaceStage(vm: AppViewModel, s: Strings, palette: List<Color>) {
    val neu = LocalNeu.current
    val names = vm.vlsmTask.requirements.map { it.name }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        vm.orderedAllocations.forEach { alloc ->
            val name = alloc.requirement.name
            val tone = palette[names.indexOf(name).coerceAtLeast(0) % palette.size]
            val answer = vm.answerFor(name)
            val grades = if (vm.stageChecked) vm.vlsmGrades[name].orEmpty() else emptyMap()
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
                    Text(
                        "/${alloc.prefix}  ·  ${s.num(alloc.blockSize)}",
                        style = NeuType.Mono,
                        color = neu.faint,
                    )
                }
                Spacer(Modifier.height(12.dp))
                AnswerField(
                    label = s.fieldNetwork,
                    value = answer.network,
                    onValueChange = { vm.updateVlsm(name, VlsmField.Network, it) },
                    state = grades[VlsmField.Network],
                    expected = Grader.expected(VlsmField.Network, alloc),
                    s = s,
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
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
                Spacer(Modifier.height(9.dp))
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
}

@Composable
private fun AnalyzeStageBody(vm: AppViewModel, s: Strings) {
    val task = vm.analyzeTask
    val grades = if (vm.stageChecked) vm.analyzeGrades else emptyMap()
    NeuCard {
        SectionLabel(s.yourAnswer)
        vm.analyzeStage.fields.forEachIndexed { index, field ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            AnswerField(
                label = when (field) {
                    AnalyzeField.Network -> s.fieldNetwork
                    AnalyzeField.Mask -> s.fieldMask
                    AnalyzeField.BlockSize -> s.fieldBlockSize
                    AnalyzeField.FirstHost -> s.fieldFirstHost
                    AnalyzeField.LastHost -> s.fieldLastHost
                    AnalyzeField.Broadcast -> s.fieldBroadcast
                    AnalyzeField.Hosts -> s.fieldUsableHosts
                },
                value = vm.analyzeAnswer[field],
                onValueChange = { vm.updateAnalyze(field, it) },
                state = grades[field],
                expected = Grader.expected(field, task),
                s = s,
                numeric = field == AnalyzeField.Hosts || field == AnalyzeField.BlockSize,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hint, verdict, actions
// ---------------------------------------------------------------------------

@Composable
private fun HintCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val lines = when (vm.settings.kind) {
        ExerciseKind.Vlsm -> s.vlsmStageHint(vm.vlsmStage)
        ExerciseKind.Analyze -> s.analyzeStageHint(vm.analyzeStage)
    }
    NeuCard(padding = 16.dp) {
        Text(s.hintTitle.uppercase(), style = NeuType.Section, color = neu.accent)
        Spacer(Modifier.height(10.dp))
        lines.forEachIndexed { index, line ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Row {
                Box(Modifier.padding(top = 7.dp)) { Dot(neu.accent, 5.dp) }
                Spacer(Modifier.width(10.dp))
                Text(line, style = NeuType.Small.copy(lineHeight = 19.sp), color = neu.dim)
            }
        }
    }
}

@Composable
private fun Verdict(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    if (!vm.stageChecked) return
    when {
        vm.stageRevealed -> Banner(s.stageRevealed, neu.warn)
        vm.stagePassed -> Banner(s.stageCorrect, neu.ok)
        vm.stageIsOrder -> Banner(s.stageWrongOrder, neu.err)
        vm.stageWrongCount > 0 -> Banner(s.stageWrongFields(vm.stageWrongCount), neu.err)
        else -> Banner(s.stageIncomplete, neu.warn)
    }
}

@Composable
private fun StageActions(vm: AppViewModel, s: Strings, hintOpen: Boolean, onToggleHint: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (vm.stagePassed) {
            NeuButton(
                text = s.actionContinue,
                onClick = vm::continueStage,
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                tone = ButtonTone.Accent,
                fill = true,
            )
        } else {
            NeuButton(
                text = if (vm.stageChecked) s.actionRetry else s.actionCheck,
                onClick = vm::checkStage,
                icon = Icons.Rounded.Done,
                tone = ButtonTone.Accent,
                fill = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeuButton(
                text = if (hintOpen) s.actionHideHint else s.actionHint,
                onClick = onToggleHint,
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            NeuButton(
                text = s.actionShowAnswer,
                onClick = vm::revealStage,
                enabled = !vm.stagePassed,
                modifier = Modifier.weight(1f),
                fill = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeuButton(
                text = s.actionBack,
                onClick = vm::previousStage,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                enabled = vm.stageNumber > 1,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            NeuButton(
                text = s.actionNewExercise,
                onClick = vm::newExercise,
                icon = Icons.Rounded.Refresh,
                modifier = Modifier.weight(1f),
                fill = true,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// The end of an exercise
// ---------------------------------------------------------------------------

@Composable
private fun ResultCard(vm: AppViewModel, s: Strings, palette: List<Color>) {
    val neu = LocalNeu.current
    NeuCard {
        Text(s.resultTitle, style = NeuType.Title, color = neu.text)
        Spacer(Modifier.height(8.dp))
        Text(
            if (vm.mistakes == 0) s.resultPerfect else s.resultMistakes(vm.mistakes),
            style = NeuType.Small.copy(lineHeight = 18.sp),
            color = if (vm.mistakes == 0) neu.ok else neu.warn,
        )

        if (vm.settings.kind == ExerciseKind.Vlsm) {
            val plan = vm.vlsmTask.plan
            val names = vm.vlsmTask.requirements.map { it.name }
            val segments = plan.allocations.map { alloc ->
                PlanSegment(
                    alloc.requirement.name,
                    alloc.blockSize,
                    palette[names.indexOf(alloc.requirement.name).coerceAtLeast(0) % palette.size],
                )
            } + if (plan.free > 0) {
                listOf(PlanSegment(s.resultFree, plan.free, neu.faint.copy(alpha = 0.35f)))
            } else {
                emptyList()
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel(s.resultPlan)
            PlanBar(plan.capacity, segments)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Column {
                    plan.inTaskOrder.forEach { alloc ->
                        InfoRow(
                            alloc.requirement.name,
                            "${Ip.cidr(alloc.network, alloc.prefix)}  ·  bc ${Ip.format(alloc.broadcast)}",
                        )
                    }
                }
            }
        } else {
            val task = vm.analyzeTask
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Column {
                    InfoRow(s.fieldMask, Ip.format(task.mask))
                    InfoRow(s.fieldNetwork, Ip.format(task.network))
                    InfoRow(
                        s.fieldRange,
                        "${Ip.format(task.firstHost ?: task.network)} – ${Ip.format(task.lastHost ?: task.broadcast)}",
                    )
                    InfoRow(s.fieldBroadcast, Ip.format(task.broadcast))
                    InfoRow(s.fieldUsableHosts, s.num(task.usable))
                }
            }
        }
    }
}

@Composable
private fun DoneActions(vm: AppViewModel, s: Strings) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NeuButton(
            text = s.actionNewExercise,
            onClick = vm::newExercise,
            icon = Icons.Rounded.Refresh,
            tone = ButtonTone.Accent,
            fill = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeuButton(
                text = s.actionSolution,
                onClick = vm::openSolution,
                icon = Icons.Rounded.School,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            NeuButton(
                text = s.actionBack,
                onClick = vm::previousStage,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                modifier = Modifier.weight(1f),
                fill = true,
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
 * send the learner looking for a number they were one digit away from.
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
