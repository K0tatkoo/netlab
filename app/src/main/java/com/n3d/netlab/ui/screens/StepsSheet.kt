package com.n3d.netlab.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.i18n.BitsView
import com.n3d.netlab.i18n.StepCard
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.components.BitsLegend
import com.n3d.netlab.ui.components.BitsStrip
import com.n3d.netlab.ui.components.FormulaBlock
import com.n3d.netlab.ui.components.InfoRow
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuIconButton
import com.n3d.netlab.ui.components.NeuSegmented
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

/**
 * The walkthrough.
 *
 * Two reading modes, because they serve different moments: one step at a time
 * for working through a problem you are stuck on, and the whole thing at once
 * for checking a method you already half-know. The step mode is the default —
 * the point of the feature is being walked through it, and a wall of eleven
 * cards is the same thing as no explanation at all.
 */
@Composable
fun StepsSheet(
    title: String,
    cards: List<StepCard>,
    s: Strings,
    onClose: () -> Unit,
) {
    val neu = LocalNeu.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val current = index.coerceIn(0, (cards.size - 1).coerceAtLeast(0))

    // No window insets here: this renders inside AppRoot's content area, which
    // is already inset below the status bar and above the navigation bar.
    Column(
        Modifier
            .fillMaxSize()
            .background(neu.bg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = NeuType.Title, color = neu.text, maxLines = 1)
                if (!showAll && cards.isNotEmpty()) {
                    Text(
                        s.stepOf(current + 1, cards.size),
                        style = NeuType.Small,
                        color = neu.faint,
                    )
                }
            }
            NeuIconButton(Icons.Rounded.Close, s.actionClose, onClose)
        }

        NeuSegmented(
            options = listOf(false, true),
            selected = showAll,
            onSelect = { showAll = it },
            label = { if (it) s.actionShowAll else s.actionOneByOne },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (cards.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else if (showAll) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(cards) { i, card ->
                    StepCardView(card, s, "${i + 1}")
                }
            }
        } else {
            val listState = rememberLazyListState()
            // Re-reading a long step from the middle after tapping Next is
            // disorienting; every step starts at its own beginning.
            LaunchedEffect(current) { listState.scrollToItem(0) }
            LazyColumn(
                Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 16.dp,
                ),
            ) {
                item { StepCardView(cards[current], s, "${current + 1}") }
            }

            StepDots(cards.size, current, onSelect = { index = it })

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeuButton(
                    text = s.actionBack,
                    onClick = { index = (current - 1).coerceAtLeast(0) },
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    enabled = current > 0,
                    modifier = Modifier.weight(1f),
                    fill = true,
                )
                NeuButton(
                    text = if (current == cards.lastIndex) s.actionClose else s.actionNext,
                    onClick = {
                        if (current == cards.lastIndex) onClose() else index = current + 1
                    },
                    icon = if (current == cards.lastIndex) null else Icons.AutoMirrored.Rounded.ArrowForward,
                    tone = ButtonTone.Accent,
                    modifier = Modifier.weight(1f),
                    fill = true,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StepDots(total: Int, current: Int, onSelect: (Int) -> Unit) {
    val neu = LocalNeu.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val done = i <= current
            val width by animateFloatAsState(
                if (i == current) 20f else 7f,
                tween(220),
                label = "dot",
            )
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = width.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (done) neu.accent else neu.dim.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(i) },
            )
        }
    }
}

@Composable
fun StepCardView(card: StepCard, s: Strings, number: String? = null) {
    val neu = LocalNeu.current
    NeuCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (number != null) {
                Box(
                    Modifier
                        .size(28.dp)
                        .neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        number,
                        style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                        color = neu.accent,
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                card.title,
                style = NeuType.Title.copy(fontSize = 19.sp),
                color = neu.text,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        if (card.rule != null) {
            RuleLine(card.rule, s)
            Spacer(Modifier.height(12.dp))
        }

        card.body.forEach { paragraph ->
            Text(
                paragraph,
                style = NeuType.Body.copy(lineHeight = 22.sp),
                color = neu.dim,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        if (card.formula != null) {
            Spacer(Modifier.height(4.dp))
            FormulaBlock(card.formula)
            Spacer(Modifier.height(6.dp))
        }

        if (card.bits != null) {
            Spacer(Modifier.height(10.dp))
            BitsBlock(card.bits, s)
        }

        if (card.rows.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Column {
                    card.rows.forEach { (label, value) ->
                        InfoRow(label, value)
                    }
                }
            }
        }
    }
}

/**
 * The general rule, printed above this step's own numbers.
 *
 * Without it the walkthrough reveals an answer; with it, the answer is an
 * instance of something the learner can carry to the next exercise.
 */
@Composable
private fun RuleLine(rule: String, s: Strings) {
    val neu = LocalNeu.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeuRadius.Sm))
            .background(neu.accent.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(s.ruleLabel.uppercase(), style = NeuType.Section, color = neu.accent)
        Spacer(Modifier.height(4.dp))
        Text(rule, style = NeuType.Small.copy(lineHeight = 18.sp), color = neu.dim)
    }
}

@Composable
private fun BitsBlock(bits: BitsView, s: Strings) {
    Column(Modifier.fillMaxWidth()) {
        BitsStrip(bits.bits, bits.networkBits, caption = bits.caption)
        Spacer(Modifier.height(8.dp))
        BitsLegend(s.bitsLegendNetwork, s.bitsLegendHost)
    }
}
