package com.n3d.netlab.ui.theme

import android.app.Activity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.n3d.netlab.data.ThemeMode

val LocalNeu = staticCompositionLocalOf { LightNeu }

/**
 * Type scale.
 *
 * Tight letter-spacing on the headings and a genuinely monospaced face for
 * every number: readings that change every few seconds must not reflow the
 * layout as digits change width, which is exactly what a proportional font does
 * to a CPU percentage counting 9 → 10 → 9.
 */
object NeuType {
    val Display = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
    val Title = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val Section = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val Label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Small = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium)

    val Metric = TextStyle(
        fontSize = 27.sp, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, letterSpacing = (-1).sp,
    )
    val MetricSm = TextStyle(
        fontSize = 17.sp, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, letterSpacing = (-0.5).sp,
    )
    val Mono = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    val MonoSm = TextStyle(fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
}

/**
 * Motion, lifted from theme.css the same way the palette was.
 *
 * `--ease: cubic-bezier(.34, 1.4, .64, 1)` overshoots slightly before settling,
 * which is what makes a sliding neumorphic tile read as a physical thing that
 * was pushed rather than a rectangle that was repositioned. Keep the overshoot
 * for geometry only — running a colour through a curve whose output leaves
 * 0..1 interpolates past the target colour, which browsers clamp and Compose
 * does not.
 */
object NeuMotion {
    val Ease = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

    /** `.38s`, the segmented control's travel time on every site. */
    const val SlideMs = 380

    /** `--t: .22s`, used for the colour swaps that ride along with it. */
    const val FadeMs = 220
}

@Composable
fun NetLabTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val neu = if (dark) DarkNeu else LightNeu

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Icons must contrast with the *surface*, which in light mode is a
            // pale grey the system would otherwise assume is dark.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    // Material3 is present only for the handful of primitives reused below
    // (Text, Surface, ripple-free interaction). Everything visual comes from
    // NeuColors; the scheme is mapped across so no stray component paints
    // itself in Material purple.
    val scheme = if (dark) {
        darkColorScheme(
            primary = neu.accent, onPrimary = neu.text,
            background = neu.bg, onBackground = neu.text,
            surface = neu.bg, onSurface = neu.text,
            error = neu.err, onError = neu.text,
        )
    } else {
        lightColorScheme(
            primary = neu.accent, onPrimary = neu.light,
            background = neu.bg, onBackground = neu.text,
            surface = neu.bg, onSurface = neu.text,
            error = neu.err, onError = neu.light,
        )
    }

    CompositionLocalProvider(LocalNeu provides neu) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                bodyMedium = NeuType.Body.copy(color = neu.text),
                titleMedium = NeuType.Title.copy(color = neu.text),
                labelMedium = NeuType.Label.copy(color = neu.dim),
            ),
            content = content,
        )
    }
}

/** Long unit names and MOTDs must never push a row's numbers off screen. */
val Ellipsis = TextOverflow.Ellipsis
