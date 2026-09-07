package com.n3d.netlab.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * A column whose rows can be dragged into a different order.
 *
 * Every row is the same fixed height, which is what makes this simple: the
 * distance between two slots is a constant, so "which slot is the finger over"
 * is one division rather than a hit test against measured bounds.
 *
 * **There are two ways to pick a row up**, because the thing a learner sees as
 * "the subnet" is the whole row and not the two grey lines at the end of it.
 * The [handle] modifier starts a drag as soon as the finger moves: nothing else
 * lives there, so there is nothing to disambiguate. The [body] modifier starts
 * one after a long press, which is what keeps the screen scrollable — an
 * immediate drag on the row itself would win the pointer from the enclosing
 * scroll container, and the page would stop scrolling wherever a card happened
 * to be. The two must be given to *disjoint* parts of the row: one pointer can
 * then only ever be talking to one of them.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    itemHeight: Dp,
    onMove: (from: Int, to: Int) -> Unit,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    enabled: Boolean = true,
    row: @Composable (item: T, index: Int, dragging: Boolean, handle: Modifier, body: Modifier) -> Unit,
) {
    val stepPx = with(LocalDensity.current) { (itemHeight + spacing).toPx() }
    val move by rememberUpdatedState(onMove)
    val haptics = LocalHapticFeedback.current
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val count = items.size

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { index, item ->
            key(key(item)) {
                val dragging = dragIndex == index
                // The row's index changes *during* a drag, as the list
                // reorders under the finger. Keying pointerInput on it would
                // tear the gesture detector down and rebuild it mid-drag, and
                // the drag would die the moment two rows swapped — so the index
                // is read through a snapshot instead, and only at drag start.
                val startIndex by rememberUpdatedState(index)

                val start: (Offset) -> Unit = {
                    dragIndex = startIndex
                    dragOffset = 0f
                    // A long press that produces no bump feels like a press
                    // that did not register, and this is the one gesture in the
                    // app with nothing else to confirm it.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                val stop = {
                    dragIndex = -1
                    dragOffset = 0f
                }
                val drag: (PointerInputChange, Offset) -> Unit = { change, delta ->
                    change.consume()
                    dragOffset += delta.y
                    val from = dragIndex
                    if (from >= 0) {
                        val target = (from + (dragOffset / stepPx).roundToInt())
                            .coerceIn(0, count - 1)
                        if (target != from) {
                            move(from, target)
                            // The row has just been re-slotted under the
                            // finger, so the same finger position is now that
                            // many slots less of an offset from home.
                            dragOffset -= (target - from) * stepPx
                            dragIndex = target
                        }
                    }
                }

                val handle = Modifier.pointerInput(count, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(start, stop, stop, drag)
                }
                val body = Modifier.pointerInput(count, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGesturesAfterLongPress(start, stop, stop, drag)
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) },
                ) {
                    row(item, index, dragging, handle, body)
                }
            }
        }
    }
}
