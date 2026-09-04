package com.n3d.netlab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuColors
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

/**
 * The 32 bits of an address or mask, as cells.
 *
 * Two things are encoded at once, because they are the two things a learner has
 * to hold in their head simultaneously: colour says whether a bit belongs to
 * the network or the host, and fill says whether it is a 1 or a 0. The boundary
 * between the two colours is the prefix, made visible.
 */
@Composable
fun BitsStrip(
    bits: String,
    networkBits: Int,
    modifier: Modifier = Modifier,
    caption: String? = null,
    showText: Boolean = true,
) {
    val neu = LocalNeu.current
    Column(modifier.fillMaxWidth()) {
        if (caption != null) {
            Text(
                caption,
                style = NeuType.Small,
                color = neu.faint,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            )
        }
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val groupGap = size.width * 0.018f
            val cellW = (size.width - groupGap * 3) / 32f
            val gapWithin = cellW * 0.14f
            for (i in 0 until 32) {
                val x = i * cellW + (i / 8) * groupGap
                val isNetwork = i < networkBits
                val base = if (isNetwork) neu.accent else neu.dim
                val on = bits.getOrElse(i) { '0' } == '1'
                drawRoundRect(
                    color = if (on) base else base.copy(alpha = 0.18f),
                    topLeft = Offset(x, 0f),
                    size = Size((cellW - gapWithin).coerceAtLeast(1f), size.height),
                    cornerRadius = CornerRadius(2.5f, 2.5f),
                )
            }
            // The prefix boundary itself, so /26 is something you can point at.
            if (networkBits in 1..31) {
                val x = networkBits * cellW + (networkBits / 8) * groupGap - gapWithin / 2f
                drawRect(
                    color = neu.text.copy(alpha = 0.45f),
                    topLeft = Offset(x - 1f, -3f),
                    size = Size(2f, size.height + 6f),
                )
            }
        }
        if (showText) {
            Spacer(Modifier.height(6.dp))
            Text(
                bits.chunked(8).joinToString("."),
                style = NeuType.MonoSm,
                color = neu.faint,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BitsLegend(networkWord: String, hostWord: String, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Dot(neu.accent, 7.dp)
        Spacer(Modifier.width(5.dp))
        Text(networkWord, style = NeuType.Small, color = neu.faint)
        Spacer(Modifier.width(14.dp))
        Dot(neu.dim, 7.dp)
        Spacer(Modifier.width(5.dp))
        Text(hostWord, style = NeuType.Small, color = neu.faint)
    }
}

data class PlanSegment(val label: String, val size: Long, val color: Color)

/**
 * The base network drawn to scale, with each allocated block taking its real
 * share of the width.
 *
 * Every block is a power of two, so the picture is honest at any zoom — a /27
 * really is half a /26 on screen. Seeing the free tail at the end is usually
 * the moment the point of VLSM lands.
 */
@Composable
fun PlanBar(
    capacity: Long,
    segments: List<PlanSegment>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 30.dp,
) {
    val neu = LocalNeu.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .neuInset(radius = NeuRadius.Sm, depth = NeuDepths.InsetSm)
            .padding(4.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (capacity <= 0) return@Canvas
            var x = 0f
            val radius = CornerRadius(4f, 4f)
            segments.forEach { segment ->
                val w = size.width * (segment.size.toFloat() / capacity)
                if (w > 0.5f) {
                    drawRoundRect(
                        color = segment.color,
                        topLeft = Offset(x, 0f),
                        size = Size((w - 2f).coerceAtLeast(1f), size.height),
                        cornerRadius = radius,
                    )
                }
                x += w
            }
        }
        if (segments.isEmpty()) {
            Text(
                "",
                style = NeuType.Small,
                color = neu.faint,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * Colours for the plan bar and the subnet cards.
 *
 * Six hues that stay distinguishable in both themes, reused in the same order
 * everywhere so subnet C is the same colour on the bar, on its card and in the
 * walkthrough.
 */
fun segmentPalette(neu: NeuColors): List<Color> = listOf(
    neu.accent,
    neu.ok,
    neu.warn,
    neu.err,
    if (neu.isDark) Color(0xFF57B7E8) else Color(0xFF2C8BC4),
    if (neu.isDark) Color(0xFFD98FD0) else Color(0xFFA850A0),
)

@Composable
fun PlanLegend(segments: List<PlanSegment>, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(segment.color, 7.dp)
                Spacer(Modifier.width(5.dp))
                Text(segment.label, style = NeuType.Small, color = neu.dim, maxLines = 1)
            }
        }
    }
}
