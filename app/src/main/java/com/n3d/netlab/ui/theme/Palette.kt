package com.n3d.netlab.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The house palette, lifted token-for-token from theme.css so the app reads as
 * part of the same family as n3d-store, Strings, Convert, Status, Sentinel and the
 * portfolio.
 *
 * Neumorphism only works if `surface`, `dark` and `light` stay in their exact
 * relationship: the raised look is nothing but a dark shadow on the side away
 * from the light and a white one on the side facing it, over a surface that is
 * the *same* colour as its background. Nudge any of the three and every card on
 * every screen goes muddy at once.
 */
@Immutable
data class NeuColors(
    val bg: Color,
    val dark: Color,
    val light: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val err: Color,
    val errSoft: Color,
    val isDark: Boolean,
)

val LightNeu = NeuColors(
    // A neutral with a hint of blue reads as "surface" rather than "dirty
    // white", and leaves room for a genuinely white highlight.
    bg = Color(0xFFE7ECF3),
    dark = Color(0xFFC3CBD9),
    light = Color(0xFFFFFFFF),
    text = Color(0xFF262C3A),
    dim = Color(0xFF626D84),
    faint = Color(0xFF8B95A8),
    accent = Color(0xFF6C5CE7),
    accentSoft = Color(0x266C5CE7),
    ok = Color(0xFF129D6E),
    okSoft = Color(0x26129D6E),
    warn = Color(0xFFD98324),
    warnSoft = Color(0x26D98324),
    err = Color(0xFFD94A4A),
    errSoft = Color(0x26D94A4A),
    isDark = false,
)

val DarkNeu = NeuColors(
    // Dark neumorphism has to sit on a mid grey. On near-black there is no room
    // for a shadow darker than the surface, and every element flattens out.
    bg = Color(0xFF272B33),
    dark = Color(0xFF1B1E24),
    light = Color(0xFF333944),
    text = Color(0xFFEAEEF6),
    dim = Color(0xFFA2ACC0),
    faint = Color(0xFF7C869A),
    accent = Color(0xFF8B7CFF),
    accentSoft = Color(0x2E8B7CFF),
    ok = Color(0xFF35C88F),
    okSoft = Color(0x2E35C88F),
    warn = Color(0xFFE8A34A),
    warnSoft = Color(0x2EE8A34A),
    err = Color(0xFFF0706E),
    errSoft = Color(0x2EF0706E),
    isDark = true,
)
