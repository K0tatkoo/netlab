package com.n3d.netlab.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
 * The drag lives on a handle rather than on the whole row. An immediate drag
 * gesture on the row itself would win the pointer from the enclosing scroll
 * container, and the screen would stop scrolling wherever a card happened to
 * be — restricting it to the handle means a finger anywhere else still
 * scrolls the page.
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
    row: @Composable (item: T, index: Int, dragging: Boolean, handle: Modifier) -> Unit,
) {
    val stepPx = with(LocalDensity.current) { (itemHeight + spacing).toPx() }
    val move by rememberUpdatedState(onMove)
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
                val handle = Modifier.pointerInput(count, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            dragIndex = startIndex
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            dragIndex = -1
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            dragIndex = -1
                            dragOffset = 0f
                        },
                    ) { change, delta ->
                        change.consume()
                        dragOffset += delta.y
                        val from = dragIndex
                        if (from < 0) return@detectDragGestures
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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) },
                ) {
                    row(item, index, dragging, handle)
                }
            }
        }
    }
}