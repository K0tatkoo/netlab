package com.n3d.netlab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuMotion
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.ui.theme.neuRaised
import kotlin.math.roundToInt

/**
 * A segmented control whose selection slides between the options.
 *
 * The track is a well and the selected segment is a raised tile inside it, so
 * the control reads as one physical object rather than a row of buttons. The
 * indicator moves on the house easing curve, which overshoots very slightly —
 * that overshoot is what makes it feel pushed rather than repositioned.
 */
@Composable
fun <T> NeuSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeu.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .neuInset(radius = NeuRadius.Pill, depth = NeuDepths.InsetSm)
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / options.size.coerceAtLeast(1)
        val index = options.indexOf(selected).coerceAtLeast(0)
        val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
        // Animated in pixels and applied through the lambda overload of
        // offset: the Dp overload would recompose this subtree on every frame
        // of a 380 ms slide, where the lambda only re-runs layout.
        val offsetPx by animateFloatAsState(
            targetValue = itemWidthPx * index,
            animationSpec = tween(NeuMotion.SlideMs, easing = NeuMotion.Ease),
            label = "segment",
        )
        Box(
            Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .neuRaised(radius = NeuRadius.Pill, depth = NeuDepths.Sm),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { option ->
                val active = option == selected
                val tint by animateColorAsState(
                    if (active) neu.accent else neu.faint,
                    tween(NeuMotion.FadeMs),
                    label = "segmentTint",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = NeuType.Label.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A slider over a normalised 0..1 value.
 *
 * Press and drag are handled in one gesture loop rather than as a tap detector
 * plus a drag detector. Two detectors on one modifier fight over the first
 * pointer, which shows up as a tap near the knob doing nothing — the single
 * loop makes a tap anywhere on the track jump the knob, and a drag track it.
 *
 * Nothing happens on touch-down, though. The calculator is a scrolling list,
 * and a finger that lands on a slider on its way down the page is a scroll:
 * the old loop set the value on the press and then held the gesture, so
 * scrolling past a slider quietly changed it. Now the knob moves on a tap that
 * lifts without moving, or once the finger has gone sideways past the touch
 * slop — the same threshold at which the list claims a vertical movement.
 */
@Composable
fun NeuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackHeight: Dp = 14.dp,
    knobSize: Dp = 28.dp,
) {
    val neu = LocalNeu.current
    val density = LocalDensity.current
    val callback by rememberUpdatedState(onValueChange)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(knobSize + 12.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val knobPx = with(density) { knobSize.toPx() }
        val travel = (widthPx - knobPx).coerceAtLeast(1f)

        // The knob follows a spring rather than the raw finger position, so a
        // stepped slider glides between its stops instead of teleporting.
        val shown by animateFloatAsState(
            targetValue = value.coerceIn(0f, 1f),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 1600f),
            label = "sliderKnob",
        )

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(travel, enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        fun at(x: Float) = ((x - knobPx / 2f) / travel).coerceIn(0f, 1f)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        }
                        if (drag == null) {
                            // Lifted without moving: a tap. Anything else means the
                            // list took the gesture for a scroll.
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (up != null && !up.pressed && !up.isConsumed) callback(at(up.position.x))
                            return@awaitEachGesture
                        }
                        callback(at(drag.position.x))
                        horizontalDrag(drag.id) { change ->
                            callback(at(change.position.x))
                            change.consume()
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(trackHeight)
                    .neuInset(radius = NeuRadius.Pill, depth = NeuDepths.InsetSm),
            ) {
                val fillColor = if (enabled) neu.accent else neu.faint
                Canvas(Modifier.fillMaxSize()) {
                    // Drawn rather than laid out: fillMaxWidth(0f) is not a
                    // legal fraction, and the fill genuinely does reach zero.
                    val w = size.width * shown
                    if (w > 0.5f) {
                        drawRoundRect(
                            color = fillColor.copy(alpha = 0.55f),
                            size = Size(w, size.height),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((travel * shown).roundToInt(), 0) }
                    .size(knobSize)
                    .neuRaised(radius = NeuRadius.Pill, depth = NeuDepths.Sm),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (enabled) neu.accent else neu.faint),
                )
            }
        }
    }
}

/** Integer slider that snaps to whole values in [range]. */
@Composable
fun NeuIntSlider(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val span = (range.last - range.first).coerceAtLeast(1)
    NeuSlider(
        value = (value - range.first).toFloat() / span,
        onValueChange = { f -> onValueChange(range.first + (f * span).roundToInt()) },
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Slider over a host count, with the small end stretched out.
 *
 * Host counts that come up in practice are clustered at the bottom — 5, 12, 30,
 * 60 — while the top of the range runs to thousands. On a linear slider every
 * useful value would sit in the first few millimetres, so the position is
 * squared: half the travel covers the first quarter of the range.
 */
@Composable
fun NeuHostSlider(
    hosts: Int,
    maxHosts: Int,
    onHostsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = kotlin.math.sqrt((hosts.toFloat() / maxHosts).coerceIn(0f, 1f))
    NeuSlider(
        value = fraction,
        onValueChange = { f -> onHostsChange((f * f * maxHosts).roundToInt().coerceAtLeast(1)) },
        modifier = modifier,
    )
}

/** − value + , for the exact number a slider cannot land on comfortably. */
@Composable
fun NeuStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    val neu = LocalNeu.current
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeuIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "−",
            onClick = onDecrement,
            enabled = canDecrement,
            size = 36.dp,
        )
        Text(
            value,
            style = NeuType.MetricSm,
            color = neu.text,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        NeuIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "+",
            onClick = onIncrement,
            enabled = canIncrement,
            size = 36.dp,
        )
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))
