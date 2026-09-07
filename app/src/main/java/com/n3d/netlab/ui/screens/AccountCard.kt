package com.n3d.netlab.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.AuthMode
import com.n3d.netlab.AuthNotice
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuTextField
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

/**
 * Signing in, in four states: who you are, the sign-in form, the emailed code,
 * and asking for a reset link.
 *
 * An account is entirely optional and the card says so before it asks for
 * anything. Everything the app teaches works signed out and offline; the only
 * thing an account buys is the same progress on a second device — so this card
 * never blocks, never nags, and lives at the bottom of Settings.
 */
@Composable
fun AccountCard(vm: AppViewModel, s: Strings) {
    when {
        vm.account != null -> SignedInCard(vm, s)
        vm.challenge != null -> CodeCard(vm, s)
        vm.authMode == AuthMode.Forgot -> ForgotCard(vm, s)
        else -> SignInCard(vm, s)
    }
}

@Composable
private fun SignedInCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val who = vm.account ?: return
    NeuCard {
        SectionLabel(s.accountTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    who.label.take(1).uppercase(),
                    style = NeuType.Title.copy(fontSize = 18.sp),
                    color = neu.accent,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    who.label,
                    style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = neu.text,
                    maxLines = 1,
                )
                Text(who.email, style = NeuType.Small, color = neu.faint, maxLines = 1)
            }
        }
        Notice(vm, s)
        Spacer(Modifier.height(14.dp))
        NeuButton(
            text = if (vm.authBusy) s.authWorking else s.actionSignOut,
            onClick = vm::signOut,
            icon = Icons.AutoMirrored.Rounded.Logout,
            enabled = !vm.authBusy,
            fill = true,
        )
    }
}

@Composable
private fun SignInCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val registering = vm.authMode == AuthMode.Register
    var email by rememberSaveable { mutableStateOf("") }
    // Never a rememberSaveable: a password does not belong in the bundle the
    // system writes to disk when the process is killed in the background.
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }

    NeuCard {
        SectionLabel(s.accountTitle)
        Text(
            s.accountGuestBody,
            style = NeuType.Small.copy(lineHeight = 18.sp),
            color = neu.dim,
        )
        Notice(vm, s)
        Spacer(Modifier.height(14.dp))

        NeuTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = s.fieldEmail,
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        NeuTextField(
            value = password,
            onValueChange = { password = it },
            label = s.fieldPassword,
            keyboardType = KeyboardType.Password,
            masked = true,
        )
        if (registering) {
            Hint(s.passwordHint)
            Spacer(Modifier.height(12.dp))
            NeuTextField(
                value = name,
                onValueChange = { name = it },
                label = s.fieldName,
            )
            Hint(s.fieldNameHint)
        }

        Error(vm, s)
        Spacer(Modifier.height(16.dp))
        NeuButton(
            text = when {
                vm.authBusy -> s.authWorking
                registering -> s.actionRegister
                else -> s.actionSignIn
            },
            onClick = { vm.beginSignIn(email, password, name) },
            icon = if (registering) Icons.Rounded.PersonAdd else Icons.AutoMirrored.Rounded.Login,
            tone = ButtonTone.Accent,
            enabled = !vm.authBusy,
            fill = true,
        )
        Spacer(Modifier.height(10.dp))
        TextLink(if (registering) s.authHaveAccount else s.authNoAccount) {
            vm.selectAuthMode(if (registering) AuthMode.SignIn else AuthMode.Register)
        }
        if (!registering) {
            Spacer(Modifier.height(6.dp))
            TextLink(s.forgotPassword) { vm.selectAuthMode(AuthMode.Forgot) }
        }
    }
}

/**
 * Step two: the six-digit code.
 *
 * Its own card rather than a box appended to the form, because the password has
 * already been accepted by this point and showing it still filled in invites
 * somebody to "fix" it and start the whole thing again.
 */
@Composable
private fun CodeCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    var code by rememberSaveable { mutableStateOf("") }

    NeuCard {
        SectionLabel(s.accountTitle)
        Text(s.mfaTitle, style = NeuType.Title.copy(fontSize = 17.sp), color = neu.text)
        Spacer(Modifier.height(6.dp))
        Text(
            s.mfaBody(vm.challengeEmail),
            style = NeuType.Small.copy(lineHeight = 18.sp),
            color = neu.dim,
        )
        Spacer(Modifier.height(14.dp))
        NeuTextField(
            value = code,
            // Codes get pasted with spaces in them, and phones like to add one.
            onValueChange = { raw -> code = raw.filter { it.isDigit() }.take(6) },
            label = s.fieldCode,
            placeholder = "000000",
            keyboardType = KeyboardType.NumberPassword,
            textStyle = NeuType.Metric.copy(fontSize = 22.sp),
        )
        Error(vm, s)
        Notice(vm, s)
        Spacer(Modifier.height(16.dp))
        NeuButton(
            text = if (vm.authBusy) s.authWorking else s.actionVerify,
            onClick = { vm.verifyCode(code) },
            tone = ButtonTone.Accent,
            enabled = !vm.authBusy,
            fill = true,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeuButton(
                text = s.actionResend,
                onClick = { code = ""; vm.resendCode() },
                enabled = !vm.authBusy,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            NeuButton(
                text = s.actionUseAnother,
                onClick = { code = ""; vm.cancelChallenge() },
                enabled = !vm.authBusy,
                modifier = Modifier.weight(1f),
                fill = true,
            )
        }
        Spacer(Modifier.height(12.dp))
        Hint(s.mfaSpam)
        Hint(s.mfaWhy)
    }
}

/**
 * Asking for a reset link.
 *
 * The answer is the same whether or not the address has an account, so this
 * never reports success or failure — only that, if there is an account,
 * something is on its way.
 */
@Composable
private fun ForgotCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    var email by rememberSaveable { mutableStateOf("") }

    NeuCard {
        SectionLabel(s.accountTitle)
        Text(s.resetTitle, style = NeuType.Title.copy(fontSize = 17.sp), color = neu.text)
        Spacer(Modifier.height(6.dp))
        Text(s.resetBody, style = NeuType.Small.copy(lineHeight = 18.sp), color = neu.dim)
        Spacer(Modifier.height(14.dp))
        NeuTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = s.fieldEmail,
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email,
        )
        Error(vm, s)
        Notice(vm, s)
        Spacer(Modifier.height(16.dp))
        NeuButton(
            text = if (vm.authBusy) s.authWorking else s.actionSendReset,
            onClick = { vm.requestPasswordReset(email) },
            icon = Icons.Rounded.MailOutline,
            tone = ButtonTone.Accent,
            enabled = !vm.authBusy,
            fill = true,
        )
        Spacer(Modifier.height(10.dp))
        TextLink(s.actionBackToSignIn) { vm.selectAuthMode(AuthMode.SignIn) }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun Error(vm: AppViewModel, s: Strings) {
    val code = vm.authError ?: return
    Spacer(Modifier.height(12.dp))
    Banner(s.authError(code), LocalNeu.current.err)
}

@Composable
private fun Notice(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val text = when (vm.authNotice) {
        AuthNotice.SignedIn -> s.authSyncedIn
        AuthNotice.SignedOut -> s.authSignedOut
        AuthNotice.CodeResent -> s.mfaResent
        AuthNotice.ResetSent -> s.resetSent(vm.challengeEmail)
        null -> return
    }
    Spacer(Modifier.height(12.dp))
    Banner(text, neu.ok)
}

@Composable
private fun Hint(text: String) {
    val neu = LocalNeu.current
    Text(
        text,
        style = NeuType.Small.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = neu.faint,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** A plain tappable line — switching between the forms is not a command. */
@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    val neu = LocalNeu.current
    Text(
        text,
        style = NeuType.Small.copy(fontWeight = FontWeight.Bold),
        color = neu.accent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
    )
}
