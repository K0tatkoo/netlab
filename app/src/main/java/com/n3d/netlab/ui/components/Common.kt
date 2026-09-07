package com.n3d.netlab.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepth
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.ui.theme.neuPressable
import com.n3d.netlab.ui.theme.neuRaised

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    radius: Dp = NeuRadius.Lg,
    depth: NeuDepth = NeuDepths.Md,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val base = if (onClick != null) {
        Modifier
            .neuPressable(interaction, radius = radius, raised = depth)
            // No ripple: a Material ripple washes straight over a neumorphic
            // surface and destroys the illusion. The depth swap is the feedback.
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier.neuRaised(radius = radius, depth = depth)
    }
    Column(modifier.then(base).padding(padding), content = content)
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val neu = LocalNeu.current
    Row(
        modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = NeuType.Section, color = neu.faint)
        trailing?.invoke()
    }
}

@Composable
fun StatusPill(text: String, tone: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tone))
        Spacer(Modifier.width(6.dp))
        Text(text, style = NeuType.Small, color = tone, maxLines = 1)
    }
}

enum class ButtonTone { Accent, Neutral, Danger }

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.Neutral,
    enabled: Boolean = true,
    fill: Boolean = false,
) {
    val neu = LocalNeu.current
    val interaction = remember { MutableInteractionSource() }
    val accent = when (tone) {
        ButtonTone.Accent -> neu.accent
        ButtonTone.Neutral -> neu.dim
        ButtonTone.Danger -> neu.err
    }
    val content = if (!enabled) neu.faint else accent
    Row(
        modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 46.dp)
            .neuPressable(interaction, radius = NeuRadius.Pill, raised = NeuDepths.Sm)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = NeuType.Label.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

@Composable
fun NeuIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true,
    size: Dp = 42.dp,
) {
    val neu = LocalNeu.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .neuPressable(interaction, radius = NeuRadius.Pill, raised = NeuDepths.Sm)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (enabled) tint ?: neu.dim else neu.faint,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** A label/value line — the workhorse of every detail panel. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    mono: Boolean = true,
) {
    val neu = LocalNeu.current
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = NeuType.Label, color = neu.faint, maxLines = 2, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = if (mono) NeuType.Mono else NeuType.Label,
            color = valueColor ?: neu.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    minHeight: Dp = 52.dp,
    textStyle: TextStyle = NeuType.Body,
    accent: Color? = null,
    /** Dots instead of the characters, for a password. */
    masked: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val neu = LocalNeu.current
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                label.uppercase(),
                style = NeuType.Section,
                color = accent ?: neu.faint,
                modifier = Modifier.padding(start = 6.dp, bottom = 7.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                // Inputs are wells, always. It is the only cue that a field can
                // be typed into when nothing on the screen has a border.
                .neuInset(radius = NeuRadius.Md, depth = NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = textStyle, color = neu.faint, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle.copy(color = accent ?: neu.text),
                    cursorBrush = SolidColor(neu.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation =
                        if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun NeuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeu.current
    val knobOffset by animateDpAsState(if (checked) 24.dp else 3.dp, label = "knob")
    Box(
        modifier
            .size(width = 52.dp, height = 30.dp)
            .neuInset(radius = NeuRadius.Pill, depth = NeuDepths.InsetSm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .padding(start = knobOffset, top = 3.dp)
                .size(24.dp)
                .neuRaised(
                    radius = NeuRadius.Pill,
                    depth = NeuDepths.InsetSm,
                    surface = if (checked) neu.accent else neu.bg,
                ),
        )
    }
}

/** Full-width tinted notice — verdicts, overflow warnings, hints. */
@Composable
fun Banner(
    text: String,
    tone: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val neu = LocalNeu.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeuRadius.Md))
            .background(tone.copy(alpha = 0.12f))
            .padding(14.dp),
    ) {
        if (title != null) {
            Text(title, style = NeuType.Label.copy(fontWeight = FontWeight.Bold), color = tone)
            Spacer(Modifier.height(3.dp))
        }
        Text(text, style = NeuType.Small, color = neu.dim)
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    val neu = LocalNeu.current
    Text(
        text,
        style = NeuType.Small,
        color = neu.faint,
        modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
        textAlign = TextAlign.Center,
    )
}

/** A monospaced line of arithmetic, set into the surface. */
@Composable
fun FormulaBlock(text: String, modifier: Modifier = Modifier, tone: Color? = null) {
    val neu = LocalNeu.current
    Box(
        modifier
            .fillMaxWidth()
            .neuInset(radius = NeuRadius.Md, depth = NeuDepths.InsetSm)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = NeuType.Mono.copy(fontWeight = FontWeight.Bold),
            color = tone ?: neu.accent,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A modal panel.
 *
 * Material's Dialog paints its own scrim and container, both of which fight the
 * neumorphic surface. This is a plain raised card over a dimmed background, so
 * a modal looks like the rest of the app rather than like a system dialog that
 * wandered in.
 */
@Composable
fun Overlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val neu = LocalNeu.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (neu.isDark) 0.55f else 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier
                .fillMaxWidth()
                .padding(24.dp)
                // Swallow taps so a press inside the card does not dismiss it.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .neuRaised(NeuRadius.Lg, NeuDepths.Lg)
                .padding(22.dp),
            content = content,
        )
    }
}
