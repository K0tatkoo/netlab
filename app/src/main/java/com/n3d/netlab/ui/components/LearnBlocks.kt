package com.n3d.netlab.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.i18n.Block
import com.n3d.netlab.i18n.RecipeStep
import com.n3d.netlab.i18n.SplitPart
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

/**
 * One block of course material.
 *
 * The course is data, not composables, so every page of both languages goes
 * through this one renderer. A page that reads well in English cannot then
 * quietly acquire a different layout in Czech.
 */
@Composable
fun LessonBlock(block: Block, s: Strings, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    when (block) {
        is Block.Para -> Text(
            block.text,
            style = NeuType.Body.copy(lineHeight = 23.sp),
            color = neu.dim,
            modifier = modifier,
        )

        is Block.Heading -> Text(
            block.text.uppercase(),
            style = NeuType.Section,
            color = neu.faint,
            modifier = modifier.padding(top = 6.dp),
        )

        is Block.Bullets -> Column(modifier.fillMaxWidth()) {
            block.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Box(Modifier.padding(top = 7.dp)) { Dot(neu.accent, 6.dp) }
                    Spacer(Modifier.width(10.dp))
                    Text(item, style = NeuType.Small.copy(lineHeight = 19.sp), color = neu.dim)
                }
            }
        }

        is Block.Table -> Box(
            modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column {
                block.rows.forEach { (label, value) -> InfoRow(label, value) }
            }
        }

        is Block.Formula -> FormulaBlock(block.text, modifier)

        is Block.Note -> Banner(block.text, neu.warn, modifier)

        is Block.Tip -> Banner(block.text, neu.ok, modifier)

        is Block.Bits -> Column(modifier.fillMaxWidth()) {
            BitsStrip(block.bits, block.networkBits, caption = block.caption)
            Spacer(Modifier.height(8.dp))
            BitsLegend(s.bitsLegendNetwork, s.bitsLegendHost)
        }

        is Block.Work -> WorkBlock(block.caption, block.lines, modifier)

        is Block.Recipe -> RecipeList(block.items, modifier)

        is Block.Split -> SplitVisual(block.caption, block.capacity, block.parts, modifier)

        is Block.PlaceValue -> PlaceValueStrip(block.caption, block.value, modifier)

        is Block.Check -> CheckBlock(block, s, modifier)
    }
}

/**
 * A worked calculation, exactly as it would be written on paper.
 *
 * Monospaced and left-aligned so the columns of an AND or a long division line
 * up, and horizontally scrollable rather than wrapped: a wrapped line of
 * arithmetic is worse than one the reader has to nudge sideways, because the
 * wrap silently destroys the alignment that carries the meaning.
 */
@Composable
fun WorkBlock(caption: String?, lines: List<String>, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    Column(modifier.fillMaxWidth()) {
        if (caption != null) {
            Text(
                caption,
                style = NeuType.Small,
                color = neu.faint,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                lines.forEach { line ->
                    if (line.isEmpty()) {
                        Spacer(Modifier.height(9.dp))
                    } else {
                        Text(
                            line,
                            style = NeuType.Mono.copy(lineHeight = 18.sp),
                            color = if (line.startsWith(" ") || line.contains("<-")) neu.dim else neu.text,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/** A numbered procedure: the rule on top, this example's numbers underneath. */
@Composable
fun RecipeList(items: List<RecipeStep>, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, step ->
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(26.dp).neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                        color = neu.accent,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.rule, style = NeuType.Body.copy(lineHeight = 21.sp), color = neu.dim)
                    if (step.work != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(step.work, style = NeuType.Mono, color = neu.accent)
                    }
                }
            }
        }
    }
}

/**
 * A network drawn to scale and cut into its blocks.
 *
 * Every block is a power of two, so the picture is honest at any zoom — a /27
 * really is half the width of a /26. Seeing the stranded gap in the
 * wrong-order example is usually the moment the sorting rule lands.
 */
@Composable
fun SplitVisual(
    caption: String,
    capacity: Long,
    parts: List<SplitPart>,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeu.current
    val palette = segmentPalette(neu)
    var colorIndex = 0
    val segments = parts.map { part ->
        val color = if (part.free) neu.faint.copy(alpha = 0.35f) else palette[colorIndex++ % palette.size]
        PlanSegment(part.label, part.size, color)
    }
    Column(modifier.fillMaxWidth()) {
        Text(
            caption,
            style = NeuType.Small,
            color = neu.faint,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
        )
        PlanBar(capacity, segments, height = 26.dp)
        Spacer(Modifier.height(8.dp))
        Column {
            parts.forEachIndexed { index, part ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(segments[index].color, 8.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        part.label,
                        style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
                        color = neu.dim,
                        modifier = Modifier.weight(1f),
                    )
                    Text(part.detail, style = NeuType.MonoSm, color = neu.faint, maxLines = 1)
                }
            }
        }
    }
}

/**
 * The eight place-value columns of one octet, with the bits switched on.
 *
 * Binary is taught here as a row of columns to add up rather than as a string
 * of digits, because that is how it is actually done by hand.
 */
@Composable
fun PlaceValueStrip(caption: String, value: Int, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    val columns = listOf(128, 64, 32, 16, 8, 4, 2, 1)
    Column(modifier.fillMaxWidth()) {
        Text(
            caption,
            style = NeuType.Small,
            color = neu.faint,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            columns.forEach { column ->
                val on = value and column != 0
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(NeuRadius.Sm))
                            .background(if (on) neu.accent else neu.dim.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (on) "1" else "0",
                            style = NeuType.Mono.copy(fontWeight = FontWeight.Bold),
                            color = if (on) neu.light else neu.faint,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        column.toString(),
                        style = NeuType.MonoSm,
                        color = if (on) neu.accent else neu.faint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A question with the answer hidden until the reader has committed to one. */
@Composable
fun CheckBlock(block: Block.Check, s: Strings, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    var revealed by rememberSaveable(block.question) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeuRadius.Md))
            .background(neu.accent.copy(alpha = 0.10f))
            .padding(14.dp),
    ) {
        Text(s.checkYourself.uppercase(), style = NeuType.Section, color = neu.accent)
        Spacer(Modifier.height(7.dp))
        Text(block.question, style = NeuType.Body.copy(lineHeight = 21.sp), color = neu.text)
        Spacer(Modifier.height(10.dp))
        if (!revealed) {
            RevealButton(s.actionReveal) { revealed = true }
        }
        AnimatedVisibility(
            visible = revealed,
            enter = expandVertically(tween(NeuMotion.SlideMs, easing = NeuMotion.Ease)) + fadeIn(tween(NeuMotion.FadeMs)),
            exit = shrinkVertically(tween(NeuMotion.FadeMs)) + fadeOut(tween(120)),
        ) {
            Column {
                Text(
                    block.answer,
                    style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = neu.ok,
                )
                if (block.why != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(block.why, style = NeuType.Small.copy(lineHeight = 18.sp), color = neu.dim)
                }
            }
        }
    }
}

@Composable
private fun RevealButton(text: String, onClick: () -> Unit) {
    val neu = LocalNeu.current
    Box(
        Modifier
            .clip(RoundedCornerShape(NeuRadius.Pill))
            .background(neu.accent.copy(alpha = 0.16f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
            color = neu.accent,
            textAlign = TextAlign.Center,
        )
    }
}
