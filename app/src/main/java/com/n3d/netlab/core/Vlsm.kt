package com.n3d.netlab.core

/** One line of the assignment: "A needs 60 hosts". */
data class Requirement(val name: String, val hosts: Int)

/** One subnet, placed. Everything but `network` and `prefix` is derived. */
data class Allocation(
    val requirement: Requirement,
    /** Position in the order the assignment listed them — the order the UI shows. */
    val originalIndex: Int,
    /** Position in allocation order, largest block first. */
    val rank: Int,
    val prefix: Int,
    val network: Long,
) {
    val hostBits: Int get() = 32 - prefix
    val blockSize: Long get() = Ip.blockSize(prefix)
    val broadcast: Long get() = network + blockSize - 1
    val firstHost: Long get() = network + 1
    val lastHost: Long get() = broadcast - 1
    val usable: Long get() = Ip.usableHosts(prefix)
    val spare: Long get() = usable - requirement.hosts
    val mask: Long get() = Ip.mask(prefix)
}

data class VlsmPlan(
    val base: Long,
    val basePrefix: Int,
    /** Allocation order: largest subnet first. */
    val allocations: List<Allocation>,
    /** Requirements that did not fit in the space left. */
    val unplaced: List<Requirement>,
) {
    val capacity: Long get() = Ip.blockSize(basePrefix)
    val used: Long get() = allocations.sumOf { it.blockSize }
    val free: Long get() = capacity - used
    val endExclusive: Long get() = base + capacity

    /** First address after the last allocated block — where a fifth subnet would go. */
    val nextFree: Long get() = allocations.maxOfOrNull { it.broadcast + 1 } ?: base

    val fits: Boolean get() = unplaced.isEmpty()

    /** Addresses inside allocated blocks that no host will ever use. */
    val wasted: Long get() = allocations.sumOf { it.spare }

    /** The order the assignment was written in — what the answer sheet shows. */
    val inTaskOrder: List<Allocation> get() = allocations.sortedBy { it.originalIndex }

    fun forName(name: String): Allocation? = allocations.firstOrNull { it.requirement.name == name }
}

object Vlsm {

    /**
     * Allocates every requirement inside the base network, largest first.
     *
     * Largest-first is not a stylistic choice — it is the whole method. A block
     * of 2^n addresses may only start at a multiple of 2^n, so handing out a
     * /28 before a /26 leaves the /26 with nowhere aligned to land and strands
     * the space in between. Descending order makes the cursor land pre-aligned
     * every single time, which is why the walkthrough spends a whole step on it.
     */
    fun plan(base: Long, basePrefix: Int, requirements: List<Requirement>): VlsmPlan {
        val network = Ip.networkOf(base, basePrefix)
        val end = network + Ip.blockSize(basePrefix)

        val ordered = requirements
            .mapIndexed { index, req -> index to req }
            // Ties keep the order the assignment listed them in, so two subnets
            // of the same size always get the same answer twice.
            .sortedWith(compareByDescending<Pair<Int, Requirement>> { it.second.hosts }.thenBy { it.first })

        var cursor = network
        val placed = mutableListOf<Allocation>()
        val unplaced = mutableListOf<Requirement>()

        for ((rank, entry) in ordered.withIndex()) {
            val (originalIndex, req) = entry
            val prefix = Ip.prefixFor(req.hosts)
            val block = Ip.blockSize(prefix)
            // Round the cursor up to the next multiple of the block size. In
            // descending order it is already there; doing it anyway keeps the
            // allocator correct if a caller ever hands in an unsorted list.
            val aligned = ((cursor + block - 1) / block) * block
            if (aligned + block > end) {
                unplaced += req
                continue
            }
            placed += Allocation(req, originalIndex, rank, prefix, aligned)
            cursor = aligned + block
        }

        return VlsmPlan(network, basePrefix, placed, unplaced)
    }

    /** Total addresses the assignment needs once every +2 is paid. */
    fun demand(requirements: List<Requirement>): Long =
        requirements.sumOf { Ip.blockSize(Ip.prefixFor(it.hosts)) }
}
