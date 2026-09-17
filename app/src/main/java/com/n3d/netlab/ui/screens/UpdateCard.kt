package com.n3d.netlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.InfoRow
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuSwitch
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset
import com.n3d.netlab.update.UpdateManager
import com.n3d.netlab.update.UpdateState

/**
 * Keeping the app current from inside the app.
 *
 * NetLab is not on Play, so nothing else will ever tell somebody a new version
 * exists. The card is deliberately one line tall until there is actually news:
 * the version, and a button. Everything else appears only in the state it
 * belongs to.
 */
@Composable
fun UpdateCard(vm: AppViewModel, s: Strings) {
    val neu = LocalNeu.current
    val context = LocalContext.current
    val updates = vm.updates
    val state by updates.state.collectAsStateWithLifecycle()
    var auto by remember { mutableStateOf(updates.autoCheck) }

    // Coming back from the system's "install unknown apps" screen is the only
    // way that permission can change, and there is no callback for it — so the
    // file already downloaded is picked back up on the next resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.resumeInstall() }

    NeuCard {
        SectionLabel(s.updateTitle)
        InfoRow(s.updateInstalled, "NetLab ${updates.installedVersionName}")

        if (!updates.canSelfUpdate) {
            Spacer(Modifier.height(8.dp))
            Text(s.updateUnavailable, style = NeuType.Small, color = neu.faint)
            return@NeuCard
        }

        Spacer(Modifier.height(12.dp))

        when (val current = state) {
            is UpdateState.Idle -> CheckButton(s, updates)

            is UpdateState.Checking -> {
                Text(s.updateChecking, style = NeuType.Small, color = neu.dim)
            }

            is UpdateState.UpToDate -> {
                Text(s.updateUpToDate, style = NeuType.Small, color = neu.dim)
                Spacer(Modifier.height(10.dp))
                CheckButton(s, updates)
            }

            is UpdateState.Available -> {
                Banner(
                    title = s.updateFound(current.release.versionName),
                    text = current.release.notes.orEmpty(),
                    tone = neu.accent,
                )
                Spacer(Modifier.height(10.dp))
                NeuButton(
                    text = s.updateDownload(UpdateManager.formatSize(current.release.sizeBytes)),
                    onClick = { updates.download(current.release) },
                    icon = Icons.Rounded.Download,
                    tone = ButtonTone.Accent,
                    fill = true,
                )
                Spacer(Modifier.height(8.dp))
                NeuButton(text = s.actionLater, onClick = updates::dismiss, fill = true)
            }

            is UpdateState.Downloading -> {
                Text(s.updateDownloading, style = NeuType.Small, color = neu.dim)
                Spacer(Modifier.height(8.dp))
                ProgressWell(current.bytes, current.total)
                Spacer(Modifier.height(10.dp))
                NeuButton(text = s.actionCancel, onClick = updates::cancel, fill = true)
            }

            is UpdateState.Ready -> {
                Banner(title = s.updateAllowTitle, text = s.updateAllowBody, tone = neu.warn)
                Spacer(Modifier.height(10.dp))
                NeuButton(
                    text = s.updateAllow,
                    onClick = { context.startActivity(updates.unknownSourcesIntent()) },
                    tone = ButtonTone.Accent,
                    fill = true,
                )
            }

            is UpdateState.Installing -> {
                Text(s.updateOpening, style = NeuType.Small, color = neu.dim)
            }

            is UpdateState.Failed -> {
                Banner(
                    text = listOfNotNull(s.updateError(current.error), current.detail)
                        .joinToString("\n"),
                    tone = neu.err,
                )
                Spacer(Modifier.height(10.dp))
                CheckButton(s, updates)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.updateAuto,
                style = NeuType.Label,
                color = neu.dim,
                modifier = Modifier.weight(1f),
            )
            NeuSwitch(checked = auto, onCheckedChange = {
                auto = it
                updates.autoCheck = it
            })
        }
        Spacer(Modifier.height(4.dp))
        Text(s.updateAutoHint, style = NeuType.Small.copy(lineHeight = 16.sp), color = neu.faint)
    }
}

@Composable
private fun CheckButton(s: Strings, updates: UpdateManager) {
    NeuButton(
        text = s.updateCheck,
        onClick = { updates.check() },
        icon = Icons.Rounded.Refresh,
        fill = true,
    )
}

/** A sunken track with a filled bar — the same well every other reading in the
    app sits in, so progress is not the one place with a Material widget in it. */
@Composable
private fun ProgressWell(done: Long, total: Long) {
    val neu = LocalNeu.current
    val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .neuInset(NeuRadius.Pill, NeuDepths.InsetSm),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .neuInset(NeuRadius.Pill, NeuDepths.InsetSm, surface = neu.accent),
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            UpdateManager.formatSize(done),
            style = NeuType.Mono,
            color = neu.faint,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            UpdateManager.formatSize(total),
            style = NeuType.Mono,
            color = neu.faint,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
