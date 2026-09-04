package com.n3d.netlab.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RectF
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic depth.
 *
 * One rule holds the whole look together: a control is the same colour as the
 * surface behind it, and only two shadows separate them — a dark one on the
 * side away from the light and a light one on the side facing it. The light
 * source is fixed at the top-left on every screen. The moment two elements
 * disagree about where the light comes from, the illusion collapses into mush.
 *
 * Raised = shadows outside. Pressed = the same two shadows inside. There is no
 * third state and there are no borders.
 */
@Immutable
data class NeuDepth(
    val offset: Dp,
    val blur: Dp,
    val lightOffset: Dp = offset,
    val lightBlur: Dp = blur,
)

object NeuDepths {
    /** These mirror --sh-out-sm / --sh-out / --sh-out-lg / --sh-in / --sh-in-sm. */
    val Sm = NeuDepth(4.dp, 9.dp)
    val Md = NeuDepth(7.dp, 15.dp)
    val Lg = NeuDepth(12.dp, 26.dp, lightOffset = 10.dp, lightBlur = 22.dp)
    val Inset = NeuDepth(5.dp, 10.dp)
    val InsetSm = NeuDepth(3.dp, 6.dp)
}

object NeuRadius {
    val Lg = 26.dp
    val Md = 18.dp
    val Sm = 12.dp
    val Pill = 999.dp
}

/**
 * CSS blur-radius `b` describes a Gaussian with σ = b/2, while BlurMaskFilter
 * takes a radius that it converts as σ = 0.57735·r + 0.5. Converting here means
 * the shadows land at the same softness as the websites' rather than merely
 * similar, which is the difference between "same design system" and "looks a
 * bit like it".
 */
private fun blurRadiusFor(cssBlurPx: Float): Float =
    (((cssBlurPx / 2f) - 0.5f) / 0.57735f).coerceAtLeast(0.1f)

private fun shadowPaint(color: Color, blurPx: Float) = NativePaint().apply {
    isAntiAlias = true
    this.color = color.toArgb()
    maskFilter = BlurMaskFilter(blurRadiusFor(blurPx), BlurMaskFilter.Blur.NORMAL)
}

/** A raised surface: the default state of every card, tile and button. */
@Composable
fun Modifier.neuRaised(
    radius: Dp = NeuRadius.Md,
    depth: NeuDepth = NeuDepths.Md,
    surface: Color? = null,
): Modifier {
    val c = LocalNeu.current
    val fill = surface ?: c.bg
    return this.drawWithCache {
        val r = radius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val dOff = depth.offset.toPx()
        val lOff = depth.lightOffset.toPx()
        val darkPaint = shadowPaint(c.dark, depth.blur.toPx())
        val lightPaint = shadowPaint(c.light, depth.lightBlur.toPx())

        onDrawBehind {
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                nc.drawRoundRect(dOff, dOff, size.width + dOff, size.height + dOff, r, r, darkPaint)
                nc.drawRoundRect(-lOff, -lOff, size.width - lOff, size.height - lOff, r, r, lightPaint)
            }
            // The surface must be painted over the shadows, not merely near
            // them — it is the same colour as the page, and that is the trick.
            drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
        }
    }
}

/**
 * A pressed / recessed surface: inputs, wells, and anything currently active.
 *
 * The shadows are drawn as thick blurred strokes clipped to the shape, offset
 * the same way as the raised version. Offsetting down-right therefore leaves
 * the dark band along the *inside* of the top-left edge, which is what a
 * genuine dent looks like under a top-left light.
 */
@Composable
fun Modifier.neuInset(
    radius: Dp = NeuRadius.Md,
    depth: NeuDepth = NeuDepths.Inset,
    surface: Color? = null,
): Modifier {
    val c = LocalNeu.current
    val fill = surface ?: c.bg
    return this.drawWithCache {
        val r = radius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val dOff = depth.offset.toPx()
        val lOff = depth.lightOffset.toPx()

        fun band(color: Color, blurPx: Float, offset: Float) = shadowPaint(color, blurPx).apply {
            style = NativePaint.Style.STROKE
            // Wide enough that the blurred edge still reaches the boundary it
            // is supposed to be hugging.
            strokeWidth = offset * 2f + blurPx
        }

        val darkPaint = band(c.dark, depth.blur.toPx(), dOff)
        val lightPaint = band(c.light, depth.lightBlur.toPx(), lOff)
        val clip = NativePath().apply {
            addRoundRect(RectF(0f, 0f, size.width, size.height), r, r, NativePath.Direction.CW)
        }

        onDrawBehind {
            drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val save = nc.save()
                nc.clipPath(clip)
                nc.drawRoundRect(dOff, dOff, size.width + dOff, size.height + dOff, r, r, darkPaint)
                nc.drawRoundRect(-lOff, -lOff, size.width - lOff, size.height - lOff, r, r, lightPaint)
                nc.restoreToCount(save)
            }
        }
    }
}

/**
 * Raised until touched, then pressed in.
 *
 * Neumorphism has no hover and no ripple to fall back on, so this swap *is* the
 * press feedback. Without it a button on this surface gives no sign at all that
 * it registered the tap.
 */
@Composable
fun Modifier.neuPressable(
    interactionSource: InteractionSource,
    radius: Dp = NeuRadius.Md,
    raised: NeuDepth = NeuDepths.Sm,
    pressed: NeuDepth = NeuDepths.InsetSm,
    surface: Color? = null,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    return if (isPressed) neuInset(radius, pressed, surface)
    else neuRaised(radius, raised, surface)
}
