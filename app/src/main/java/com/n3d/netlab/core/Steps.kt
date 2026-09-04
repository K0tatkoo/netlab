package com.n3d.netlab.core

/**
 * The worked solution, as data.
 *
 * Every number the walkthrough will ever print is computed here once and
 * carried in these classes; the i18n layer only decides which words go around
 * them. That is what keeps the Czech and English walkthroughs from drifting
 * into two different explanations of two different calculations.
 */
sealed interface VlsmStep {

    /** What you were given and what it is worth. */
    data class Intro(
        val base: Long,
        val basePrefix: Int,
        val capacity: Long,
        val usable: Long,
        val subnetCount: Int,
        val requestedHosts: Int,
        val demand: Long,
    ) : VlsmStep

    /** Sort largest first — the step everyone skips and then gets stuck on. */
    data class Order(val ordered: List<Requirement>) : VlsmStep

    /** How big must this subnet be? */
    data class Size(val alloc: Allocation, val position: Int, val of: Int) : VlsmStep

    /** Where does it go? */
    data class Place(
        val alloc: Allocation,
        val position: Int,
        val of: Int,
        val cursorBefore: Long,
        val cursorAfter: Long,
    ) : VlsmStep

    /** A requirement that could not be placed at all. */
    data class Overflow(val requirement: Requirement, val needed: Long, val remaining: Long) : VlsmStep

    /** Add it all up and check it against the base network. */
    data class Verify(val plan: VlsmPlan) : VlsmStep
}

/**
 * Two steps per subnet — "how big" and "where" — rather than one.
 *
 * They are genuinely two different skills. Sizing is arithmetic on the host
 * count; placing is alignment against the cursor. Learners who can do one and
 * not the other need to see which half they are failing, and a single merged
 * step hides that.
 */
fun VlsmPlan.steps(): List<VlsmStep> {
    val steps = mutableListOf<VlsmStep>()
    val requirements = allocations.sortedBy { it.originalIndex }.map { it.requirement } + unplaced

    steps += VlsmStep.Intro(
        base = base,
        basePrefix = basePrefix,
        capacity = capacity,
        usable = Ip.usableHosts(basePrefix),
        subnetCount = requirements.size,
        requestedHosts = requirements.sumOf { it.hosts },
        demand = Vlsm.demand(requirements),
    )

    steps += VlsmStep.Order(
        allocations.map { it.requirement } +
            unplaced.sortedByDescending { it.hosts },
    )

    var cursor = base
    allocations.forEachIndexed { index, alloc ->
        steps += VlsmStep.Size(alloc, index + 1, allocations.size)
        steps += VlsmStep.Place(alloc, index + 1, allocations.size, cursor, alloc.broadcast + 1)
        cursor = alloc.broadcast + 1
    }

    unplaced.forEach {
        steps += VlsmStep.Overflow(it, Ip.blockSize(Ip.prefixFor(it.hosts)), endExclusive - cursor)
    }

    steps += VlsmStep.Verify(this)
    return steps
}

// ---------------------------------------------------------------------------
// Single-subnet analysis: given an address and a prefix, find everything else.
// ---------------------------------------------------------------------------

data class AnalyzeTask(val address: Long, val prefix: Int) {
    val mask: Long get() = Ip.mask(prefix)
    val wildcard: Long get() = Ip.wildcard(prefix)
    val network: Long get() = Ip.networkOf(address, prefix)
    val broadcast: Long get() = Ip.broadcastOf(address, prefix)
    val firstHost: Long? get() = Ip.firstHostOf(address, prefix)
    val lastHost: Long? get() = Ip.lastHostOf(address, prefix)
    val blockSize: Long get() = Ip.blockSize(prefix)
    val usable: Long get() = Ip.usableHosts(prefix)
    val hostBits: Int get() = 32 - prefix
    /** (octet number 1..4, step) — the "interesting octet" and its magic number. */
    val magic: Pair<Int, Long> get() = Ip.magicNumber(prefix)
}

sealed interface AnalyzeStep {
    /** Turn /n into a dotted mask. */
    data class Mask(val task: AnalyzeTask) : AnalyzeStep

    /** Find the interesting octet and the magic number. */
    data class Magic(val task: AnalyzeTask) : AnalyzeStep

    /** AND the address down to the network address. */
    data class Network(val task: AnalyzeTask, val multiple: Long) : AnalyzeStep

    /** Network + block − 1. */
    data class Broadcast(val task: AnalyzeTask) : AnalyzeStep

    /** The two addresses either side of the edges. */
    data class Hosts(val task: AnalyzeTask) : AnalyzeStep

    /** 2^h − 2, and a sanity check. */
    data class Count(val task: AnalyzeTask) : AnalyzeStep
}

fun AnalyzeTask.steps(): List<AnalyzeStep> {
    // Which multiple of the magic number the address falls into — the number the
    // learner rounds *down* to, which is the actual mechanical move being taught.
    val (octetIndex, step) = magic
    val octetValue = Ip.octet(address, octetIndex - 1).toLong()
    val multiple = (octetValue / step) * step
    return listOf(
        AnalyzeStep.Mask(this),
        AnalyzeStep.Magic(this),
        AnalyzeStep.Network(this, multiple),
        AnalyzeStep.Broadcast(this),
        AnalyzeStep.Hosts(this),
        AnalyzeStep.Count(this),
    )
}
