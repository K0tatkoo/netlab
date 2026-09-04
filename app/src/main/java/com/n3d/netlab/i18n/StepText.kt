package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.VlsmStep

/** A walkthrough step, ready to draw. */
data class StepCard(
    val title: String,
    val body: List<String>,
    val rows: List<Pair<String, String>>,
    /** One line of arithmetic, set apart in a monospaced well. */
    val formula: String? = null,
    val bits: BitsView? = null,
)

data class BitsView(val caption: String, val bits: String, val networkBits: Int)

private fun range(a: Long, b: Long) = "${Ip.format(a)} – ${Ip.format(b)}"

/**
 * Numbers and layout live here; only the prose comes from [Strings].
 *
 * Splitting it this way is what stops the two languages from turning into two
 * different explanations: a Czech learner and an English one see the same rows,
 * in the same order, holding the same values, and only the sentences differ.
 */
fun render(step: VlsmStep, s: Strings): StepCard = when (step) {

    is VlsmStep.Intro -> StepCard(
        title = s.stepIntroTitle,
        body = s.stepIntroBody(step),
        rows = listOf(
            s.baseNetwork to Ip.cidr(step.base, step.basePrefix),
            s.fieldMask to Ip.format(Ip.mask(step.basePrefix)),
            s.fieldRange to range(step.base, step.base + step.capacity - 1),
            s.fieldTotalAddresses to s.num(step.capacity),
            s.fieldUsableHosts to s.num(step.usable),
            s.rowSubnetsRequired to step.subnetCount.toString(),
            s.rowHostsRequested to s.num(step.requestedHosts),
            s.rowAddressesNeeded to s.num(step.demand),
        ),
        formula = "${s.num(step.demand)} / ${s.num(step.capacity)}",
        bits = BitsView(s.bitsCaption(step.basePrefix), Ip.bits(step.base), step.basePrefix),
    )

    is VlsmStep.Order -> StepCard(
        title = s.stepOrderTitle,
        body = s.stepOrderBody(step),
        rows = step.ordered.mapIndexed { index, req ->
            "${index + 1}.  ${req.name}" to s.hosts(req.hosts.toLong())
        },
    )

    is VlsmStep.Size -> {
        val a = step.alloc
        StepCard(
            title = s.stepSizeTitle(step),
            body = s.stepSizeBody(step),
            rows = listOf(
                s.rowHostsRequired to s.num(a.requirement.hosts),
                s.rowPlusTwo to s.num(a.requirement.hosts + 2),
                s.fieldHostBits to "${a.hostBits}  (2^${a.hostBits} = ${s.num(a.blockSize)})",
                s.fieldPrefix to "/${a.prefix}",
                s.fieldMask to Ip.format(a.mask),
                s.fieldBlockSize to s.num(a.blockSize),
                s.fieldUsableHosts to s.num(a.usable),
                s.fieldSpare to s.num(a.spare),
            ),
            formula = "2^${a.hostBits} = ${s.num(a.blockSize)} ≥ ${s.num(a.requirement.hosts + 2)}   →   /${a.prefix}",
            bits = BitsView(s.bitsCaption(a.prefix), Ip.bits(a.mask), a.prefix),
        )
    }

    is VlsmStep.Place -> {
        val a = step.alloc
        val (octetIndex, magic) = Ip.magicNumber(a.prefix)
        StepCard(
            title = s.stepPlaceTitle(step),
            body = s.stepPlaceBody(step),
            rows = listOf(
                s.rowFirstFree to Ip.format(step.cursorBefore),
                s.rowMagicOctet to octetIndex.toString(),
                s.rowMagicNumber to s.num(magic),
                s.fieldNetwork to Ip.cidr(a.network, a.prefix),
                s.fieldFirstHost to Ip.format(a.firstHost),
                s.fieldLastHost to Ip.format(a.lastHost),
                s.fieldBroadcast to Ip.format(a.broadcast),
                s.rowNextFree to Ip.format(step.cursorAfter),
            ),
            formula = "${Ip.format(a.network)} + ${s.num(a.blockSize)} − 1 = ${Ip.format(a.broadcast)}",
            bits = BitsView(s.bitsCaption(a.prefix), Ip.bits(a.network), a.prefix),
        )
    }

    is VlsmStep.Overflow -> StepCard(
        title = s.stepOverflowTitle(step),
        body = s.stepOverflowBody(step),
        rows = listOf(
            s.rowHostsRequired to s.num(step.requirement.hosts),
            s.rowAddressesNeeded to s.num(step.needed),
            s.rowFree to s.num(step.remaining),
        ),
        formula = "${s.num(step.needed)} > ${s.num(step.remaining)}",
    )

    is VlsmStep.Verify -> {
        val p = step.plan
        StepCard(
            title = s.stepVerifyTitle,
            body = s.stepVerifyBody(step),
            rows = p.inTaskOrder.map { a ->
                a.requirement.name to "${Ip.cidr(a.network, a.prefix)}   ${range(a.firstHost, a.lastHost)}"
            } + listOf(
                s.rowUsed to "${s.num(p.used)} / ${s.num(p.capacity)}",
                s.rowFree to s.num(p.free),
                s.rowWasted to s.num(p.wasted),
            ),
            formula = if (p.free > 0) range(p.nextFree, p.endExclusive - 1) else null,
        )
    }
}

fun render(step: AnalyzeStep, s: Strings): StepCard = when (step) {

    is AnalyzeStep.Mask -> {
        val t = step.task
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldPrefix to "/${t.prefix}",
                s.fieldNetworkBits to t.prefix.toString(),
                s.fieldHostBits to t.hostBits.toString(),
                s.fieldMask to Ip.format(t.mask),
                s.fieldWildcard to Ip.format(t.wildcard),
            ),
            formula = "${Ip.toBinary(t.mask)} = ${Ip.format(t.mask)}",
            bits = BitsView(s.bitsCaption(t.prefix), Ip.bits(t.mask), t.prefix),
        )
    }

    is AnalyzeStep.Magic -> {
        val t = step.task
        val (octetIndex, magic) = t.magic
        val maskOctet = Ip.octet(t.mask, octetIndex - 1)
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldMask to Ip.format(t.mask),
                s.rowMagicOctet to octetIndex.toString(),
                s.rowMagicNumber to s.num(magic),
                s.fieldBlockSize to s.num(t.blockSize),
            ),
            formula = if (t.hostBits % 8 == 0) {
                "/${t.prefix}   →   ${s.num(magic)}"
            } else {
                "256 − $maskOctet = ${s.num(magic)}"
            },
        )
    }

    is AnalyzeStep.Network -> {
        val t = step.task
        val (octetIndex, magic) = t.magic
        val octetValue = Ip.octet(t.address, octetIndex - 1)
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldAddress to Ip.format(t.address),
                s.rowMagicOctet to "$octetIndex  ($octetValue)",
                s.rowRoundedDown to s.num(step.multiple),
                s.fieldNetwork to Ip.format(t.network),
            ),
            formula = "$octetValue ÷ ${s.num(magic)} → ${octetValue / magic} × ${s.num(magic)} = ${s.num(step.multiple)}",
            bits = BitsView(s.bitsCaption(t.prefix), Ip.bits(t.address), t.prefix),
        )
    }

    is AnalyzeStep.Broadcast -> {
        val t = step.task
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldNetwork to Ip.format(t.network),
                s.fieldBlockSize to s.num(t.blockSize),
                s.fieldBroadcast to Ip.format(t.broadcast),
            ),
            formula = "${Ip.format(t.network)} + ${s.num(t.blockSize)} − 1 = ${Ip.format(t.broadcast)}",
            bits = BitsView(s.bitsCaption(t.prefix), Ip.bits(t.broadcast), t.prefix),
        )
    }

    is AnalyzeStep.Hosts -> {
        val t = step.task
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldNetwork to Ip.format(t.network),
                s.fieldFirstHost to (t.firstHost?.let { Ip.format(it) } ?: "—"),
                s.fieldLastHost to (t.lastHost?.let { Ip.format(it) } ?: "—"),
                s.fieldBroadcast to Ip.format(t.broadcast),
            ),
            formula = t.firstHost?.let { first -> range(first, t.lastHost!!) },
        )
    }

    is AnalyzeStep.Count -> {
        val t = step.task
        StepCard(
            title = s.analyzeStepTitle(step),
            body = s.analyzeStepBody(step),
            rows = listOf(
                s.fieldHostBits to t.hostBits.toString(),
                s.fieldTotalAddresses to s.num(t.blockSize),
                s.fieldUsableHosts to s.num(t.usable),
            ),
            formula = "2^${t.hostBits} − 2 = ${s.num(t.usable)}",
        )
    }
}
