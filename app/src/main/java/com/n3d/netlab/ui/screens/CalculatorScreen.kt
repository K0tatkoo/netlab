package com.n3d.netlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3d.netlab.AppViewModel
import com.n3d.netlab.core.AnalyzeTask
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.core.Vlsm
import com.n3d.netlab.core.steps
import com.n3d.netlab.i18n.Strings
import com.n3d.netlab.i18n.render
import com.n3d.netlab.ui.components.Banner
import com.n3d.netlab.ui.components.BitsLegend
import com.n3d.netlab.ui.components.BitsStrip
import com.n3d.netlab.ui.components.ButtonTone
import com.n3d.netlab.ui.components.Dot
import com.n3d.netlab.ui.components.InfoRow
import com.n3d.netlab.ui.components.NeuButton
import com.n3d.netlab.ui.components.NeuCard
import com.n3d.netlab.ui.components.NeuHostSlider
import com.n3d.netlab.ui.components.NeuIconButton
import com.n3d.netlab.ui.components.NeuIntSlider
import com.n3d.netlab.ui.components.NeuSegmented
import com.n3d.netlab.ui.components.NeuTextField
import com.n3d.netlab.ui.components.PlanBar
import com.n3d.netlab.ui.components.PlanSegment
import com.n3d.netlab.ui.components.SectionLabel
import com.n3d.netlab.ui.components.segmentPalette
import com.n3d.netlab.ui.theme.LocalNeu
import com.n3d.netlab.ui.theme.NeuDepths
import com.n3d.netlab.ui.theme.NeuRadius
import com.n3d.netlab.ui.theme.NeuType
import com.n3d.netlab.ui.theme.neuInset

private enum class CalcMode { Subnet, Designer }

private val LETTERS = listOf("A", "B", "C", "D", "E", "F", "G", "H")

/** The top of the host slider. Above this a subnet is bigger than a /20. */
private const val MAX_HOSTS = 4000

@Composable
fun CalculatorScreen(vm: AppViewModel) {
    val s = vm.strings
    val neu = LocalNeu.current
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    val mode = CalcMode.entries[modeIndex.coerceIn(0, 1)]

    var stepsOpen by rememberSaveable { mutableStateOf(false) }

    // Subnet tab
    var address by rememberSaveable { mutableStateOf("192.168.1.10") }
    var prefix by rememberSaveable { mutableIntStateOf(24) }

    // Designer tab, seeded with the textbook example so the tab is useful the
    // moment it opens rather than an empty form.
    var baseText by rememberSaveable { mutableStateOf("10.0.0.0") }
    var basePrefix by rememberSaveable { mutableIntStateOf(24) }
    var hostCounts by remember { mutableStateOf(listOf(60, 30, 14, 7)) }

    val parsedAddress = Ip.parse(address)
    val parsedBase = Ip.parse(baseText)

    if (stepsOpen) {
        val cards = remember(mode, parsedAddress, prefix, parsedBase, basePrefix, hostCounts, s) {
            when (mode) {
                CalcMode.Subnet -> parsedAddress
                    ?.let { AnalyzeTask(it, prefix).steps().map { step -> render(step, s) } }
                    .orEmpty()
                CalcMode.Designer -> parsedBase
                    ?.let { base ->
                        Vlsm.plan(
                            base,
                            basePrefix,
                            hostCounts.mapIndexed { i, h -> Requirement(LETTERS[i % LETTERS.size], h) },
                        ).steps().map { step -> render(step, s) }
                    }
                    .orEmpty()
            }
        }
        StepsSheet(s.solutionTitle, cards, s, onClose = { stepsOpen = false })
        return
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            NeuSegmented(
                options = CalcMode.entries,
                selected = mode,
                onSelect = { modeIndex = it.ordinal },
                label = { if (it == CalcMode.Subnet) s.calcModeSubnet else s.calcModeDesigner },
            )
        }

        when (mode) {
            CalcMode.Subnet -> {
                item {
                    NeuCard {
                        NeuTextField(
                            value = address,
                            onValueChange = { raw ->
                                address = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                            },
                            label = s.fieldAddress,
                            placeholder = "192.168.1.10",
                            keyboardType = KeyboardType.Decimal,
                            textStyle = NeuType.Mono.copy(fontSize = 15.sp),
                        )
                        Spacer(Modifier.height(18.dp))
                        PrefixSlider(s.fieldPrefix, prefix, 0..32) { prefix = it }
                    }
                }

                if (parsedAddress == null) {
                    item { Banner(s.calcInvalidAddress, neu.err) }
                } else {
                    val task = AnalyzeTask(parsedAddress, prefix)
                    item { SubnetResultCard(task, s) }
                    item {
                        NeuButton(
                            text = s.actionSolution,
                            onClick = { stepsOpen = true },
                            icon = Icons.Rounded.School,
                            tone = ButtonTone.Accent,
                            fill = true,
                        )
                    }
                }
            }

            CalcMode.Designer -> {
                item {
                    NeuCard {
                        NeuTextField(
                            value = baseText,
                            onValueChange = { raw ->
                                baseText = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                            },
                            label = s.designerBase,
                            placeholder = "10.0.0.0",
                            keyboardType = KeyboardType.Decimal,
                            textStyle = NeuType.Mono.copy(fontSize = 15.sp),
                        )
                        Spacer(Modifier.height(18.dp))
                        PrefixSlider(s.fieldPrefix, basePrefix, 0..30) { basePrefix = it }
                    }
                }

                itemsIndexed(hostCounts) { index, hosts ->
                    SubnetSliderCard(
                        name = LETTERS[index % LETTERS.size],
                        hosts = hosts,
                        tone = segmentPalette(neu)[index % segmentPalette(neu).size],
                        s = s,
                        onHostsChange = { value ->
                            hostCounts = hostCounts.toMutableList().also { it[index] = value }
                        },
                        onRemove = {
                            hostCounts = hostCounts.toMutableList().also { it.removeAt(index) }
                        },
                        canRemove = hostCounts.size > 1,
                    )
                }

                item {
                    NeuButton(
                        text = s.designerAddSubnet,
                        onClick = {
                            if (hostCounts.size < LETTERS.size) {
                                hostCounts = hostCounts + 10
                            }
                        },
                        icon = Icons.Rounded.Add,
                        enabled = hostCounts.size < LETTERS.size,
                        fill = true,
                    )
                }

                if (parsedBase == null) {
                    item { Banner(s.calcInvalidAddress, neu.err) }
                } else {
                    val plan = Vlsm.plan(
                        parsedBase,
                        basePrefix,
                        hostCounts.mapIndexed { i, h -> Requirement(LETTERS[i % LETTERS.size], h) },
                    )
                    item { PlanCard(plan, s) }
                    item {
                        NeuButton(
                            text = s.actionSolution,
                            onClick = { stepsOpen = true },
                            icon = Icons.Rounded.School,
                            tone = ButtonTone.Accent,
                            fill = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefixSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val neu = LocalNeu.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                style = NeuType.Section,
                color = neu.faint,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            Text("/$value", style = NeuType.MetricSm, color = neu.accent)
            Spacer(Modifier.width(12.dp))
            Text(Ip.format(Ip.mask(value)), style = NeuType.Mono, color = neu.dim)
        }
        NeuIntSlider(value = value, range = range, onValueChange = onChange)
    }
}

@Composable
private fun SubnetResultCard(task: AnalyzeTask, s: Strings) {
    val neu = LocalNeu.current
    NeuCard {
        SectionLabel(s.calcModeSubnet)
        Text(
            Ip.cidr(task.network, task.prefix),
            style = NeuType.Metric,
            color = neu.accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(14.dp))
        BitsStrip(
            Ip.bits(task.address),
            task.prefix,
            caption = s.bitsCaption(task.prefix),
        )
        Spacer(Modifier.height(8.dp))
        BitsLegend(s.bitsLegendNetwork, s.bitsLegendHost)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column {
                InfoRow(s.fieldNetwork, Ip.format(task.network))
                InfoRow(s.fieldMask, Ip.format(task.mask))
                InfoRow(s.fieldWildcard, Ip.format(task.wildcard))
                InfoRow(s.fieldFirstHost, task.firstHost?.let { Ip.format(it) } ?: "—")
                InfoRow(s.fieldLastHost, task.lastHost?.let { Ip.format(it) } ?: "—")
                InfoRow(s.fieldBroadcast, Ip.format(task.broadcast))
                InfoRow(s.fieldTotalAddresses, s.num(task.blockSize))
                InfoRow(s.fieldUsableHosts, s.num(task.usable), valueColor = neu.accent)
                InfoRow(s.fieldNetworkBits, task.prefix.toString())
                InfoRow(s.fieldHostBits, task.hostBits.toString())
                InfoRow(s.fieldClass, klassLabel(task.address, s), mono = false)
                InfoRow(s.fieldScope, scopeLabel(task.address, s), mono = false)
            }
        }
    }
}

private fun klassLabel(address: Long, s: Strings): String = when (Ip.klass(address)) {
    Ip.Klass.A -> s.classA
    Ip.Klass.B -> s.classB
    Ip.Klass.C -> s.classC
    Ip.Klass.D -> s.classD
    Ip.Klass.E -> s.classE
    Ip.Klass.Loopback -> s.classLoopback
    Ip.Klass.ThisNetwork -> s.classThisNetwork
}

private fun scopeLabel(address: Long, s: Strings): String = when (Ip.scope(address)) {
    Ip.Scope.Private -> s.scopePrivate
    Ip.Scope.Public -> s.scopePublic
    Ip.Scope.Loopback -> s.scopeLoopback
    Ip.Scope.LinkLocal -> s.scopeLinkLocal
    Ip.Scope.Cgnat -> s.scopeCgnat
    Ip.Scope.Multicast -> s.scopeMulticast
    Ip.Scope.Reserved -> s.scopeReserved
}

@Composable
private fun SubnetSliderCard(
    name: String,
    hosts: Int,
    tone: androidx.compose.ui.graphics.Color,
    s: Strings,
    onHostsChange: (Int) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    val neu = LocalNeu.current
    val prefix = Ip.prefixFor(hosts)
    NeuCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(tone, 10.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                name,
                style = NeuType.Label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = neu.text,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                s.hosts(hosts.toLong()),
                style = NeuType.MetricSm,
                color = neu.accent,
                modifier = Modifier.weight(1f),
            )
            Text(
                "/$prefix · ${s.num(Ip.usableHosts(prefix))}",
                style = NeuType.Mono,
                color = neu.faint,
            )
            Spacer(Modifier.width(6.dp))
            NeuIconButton(
                Icons.Rounded.Close,
                s.designerRemove,
                onRemove,
                enabled = canRemove,
                size = 32.dp,
            )
        }

        // A slider cannot land on 62, and 62 is exactly the number an
        // assignment asks for. So the figure is typed and the slider is for
        // exploring — the same pairing the web version settled on.
        Row(verticalAlignment = Alignment.CenterVertically) {
            HostCountField(
                hosts = hosts,
                maxHosts = MAX_HOSTS,
                label = "${s.designerHostsFor} $name",
                onHostsChange = onHostsChange,
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(12.dp))
            NeuHostSlider(
                hosts = hosts,
                maxHosts = MAX_HOSTS,
                onHostsChange = onHostsChange,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            s.designerTypeHosts,
            style = NeuType.Small,
            color = neu.faint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * The number box beside the host slider.
 *
 * It holds its own text while it has focus, so a half-typed "6" on the way to
 * "62" is not snapped back to a legal number under the finger. Leaving the
 * field is what commits it: an empty or nonsense box falls back to the count
 * the plan is already drawn from rather than showing a subnet that does not
 * exist.
 */
@Composable
private fun HostCountField(
    hosts: Int,
    maxHosts: Int,
    label: String,
    onHostsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(hosts.toString()) }
    var focused by remember { mutableStateOf(false) }

    // The slider moves the same number, so an unfocused box follows it.
    LaunchedEffect(hosts, focused) {
        if (!focused && text.toIntOrNull() != hosts) text = hosts.toString()
    }

    NeuTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(maxHosts.toString().length)
            text = digits
            digits.toIntOrNull()?.coerceIn(1, maxHosts)?.let(onHostsChange)
        },
        modifier = modifier
            .onFocusChanged { state ->
                focused = state.isFocused
                if (!state.isFocused) text = hosts.toString()
            }
            // The card's own heading says "A"; the box on its own says nothing,
            // so it carries the subnet's name for a screen reader.
            .semantics { contentDescription = label },
        keyboardType = KeyboardType.Number,
        textStyle = NeuType.Mono.copy(fontSize = 15.sp),
        minHeight = 46.dp,
    )
}

@Composable
private fun PlanCard(plan: com.n3d.netlab.core.VlsmPlan, s: Strings) {
    val neu = LocalNeu.current
    val palette = segmentPalette(neu)
    NeuCard {
        SectionLabel(s.designerPlan)
        val segments = plan.allocations
            .sortedBy { it.network }
            .map {
                PlanSegment(
                    it.requirement.name,
                    it.blockSize,
                    palette[it.originalIndex % palette.size],
                )
            }
        PlanBar(plan.capacity, segments)
        Spacer(Modifier.height(10.dp))
        Text(s.designerFits(plan.used, plan.capacity), style = NeuType.Small, color = neu.dim)
        if (plan.unplaced.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Banner(s.designerOverflow(plan.unplaced.size), neu.err)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .neuInset(NeuRadius.Md, NeuDepths.InsetSm)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column {
                plan.inTaskOrder.forEach { alloc ->
                    InfoRow(
                        alloc.requirement.name,
                        "${Ip.cidr(alloc.network, alloc.prefix)}  ·  ${Ip.format(alloc.firstHost)} – ${Ip.format(alloc.lastHost)}",
                    )
                }
                InfoRow(s.rowFree, s.num(plan.free))
                InfoRow(s.rowWasted, s.num(plan.wasted))
            }
        }
    }
}
