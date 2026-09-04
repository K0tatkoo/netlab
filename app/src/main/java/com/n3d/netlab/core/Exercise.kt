package com.n3d.netlab.core

import kotlin.random.Random

enum class Difficulty { Easy, Medium, Hard }

enum class ExerciseKind { Vlsm, Analyze }

data class VlsmTask(
    val base: Long,
    val basePrefix: Int,
    val requirements: List<Requirement>,
) {
    // Not a `get()`: the plan is read many times per frame by the practice
    // screen, and reallocating it on every read shows up as jank while typing.
    val plan: VlsmPlan by lazy { Vlsm.plan(base, basePrefix, requirements) }
    val label: String get() = Ip.cidr(base, basePrefix)
}

// ---------------------------------------------------------------------------
// Generation
// ---------------------------------------------------------------------------

object Generator {

    private val letters = listOf("A", "B", "C", "D", "E", "F", "G")

    /**
     * Builds an assignment that is guaranteed to have exactly one right answer
     * and to fit.
     *
     * It works backwards: pick the block sizes first, then invent a host count
     * that can only be satisfied by that block. Picking host counts first and
     * hoping they fit produces unsolvable assignments often enough to matter,
     * and — worse — assignments where two different block sizes both work.
     */
    fun vlsm(difficulty: Difficulty, random: Random = Random.Default): VlsmTask {
        val (basePrefix, count) = when (difficulty) {
            Difficulty.Easy -> 24 to random.nextInt(3, 5)
            Difficulty.Medium -> listOf(24, 23, 22).random(random) to random.nextInt(4, 6)
            Difficulty.Hard -> listOf(22, 21, 20).random(random) to random.nextInt(5, 7)
        }
        val base = randomBase(basePrefix, difficulty, random)
        val capacity = Ip.blockSize(basePrefix)

        val bits = mutableListOf<Int>()
        var remaining = capacity
        var ceiling = 32 - basePrefix - 1 // never let one subnet swallow the base
        for (i in 0 until count) {
            val others = count - i - 1
            // Every subnet still to come needs at least a /30's four addresses.
            val budget = remaining - 4L * others
            var maxBits = ceiling.coerceAtMost(30)
            while (maxBits > 2 && (1L shl maxBits) > budget) maxBits--
            if (maxBits < 2) break
            // Skew towards the top of the range: an assignment of five /30s is
            // arithmetically valid and pedagogically worthless.
            val low = (maxBits - 2).coerceAtLeast(2)
            val h = random.nextInt(low, maxBits + 1)
            bits += h
            remaining -= (1L shl h)
            ceiling = h
        }

        val requirements = bits.mapIndexed { index, h ->
            val max = (1L shl h) - 2
            val min = ((1L shl (h - 1)) - 2 + 1).coerceAtLeast(1)
            // The exact-fit numbers (62, 30, 14, 6) are the ones textbooks use
            // and the ones learners misjudge, so they come up often on purpose.
            val hosts = if (random.nextInt(100) < 35) max else random.nextLong(min, max + 1)
            Requirement(letters[index], hosts.toInt())
        }

        // Present them shuffled: sorting largest-first is a step the learner is
        // supposed to perform, not something the assignment does for them.
        return VlsmTask(base, basePrefix, requirements.shuffled(random))
    }

    fun analyze(difficulty: Difficulty, random: Random = Random.Default): AnalyzeTask {
        val prefix = when (difficulty) {
            // Fourth octet only: the mask is 255.255.255.x and the arithmetic
            // stays inside one octet.
            Difficulty.Easy -> random.nextInt(25, 31)
            // Crosses into the third octet, where most people first go wrong.
            Difficulty.Medium -> random.nextInt(17, 31)
            Difficulty.Hard -> random.nextInt(9, 31)
        }
        // An address that is deliberately *not* the network address — finding
        // which block it belongs to is the exercise.
        val network = randomBase(prefix, difficulty, random)
        val offset = if (Ip.blockSize(prefix) > 2) random.nextLong(1, Ip.blockSize(prefix) - 1) else 0L
        return AnalyzeTask(network + offset, prefix)
    }

    private fun randomBase(prefix: Int, difficulty: Difficulty, random: Random): Long {
        val raw = when (difficulty) {
            Difficulty.Easy -> when (random.nextInt(2)) {
                0 -> Ip.parse("192.168.${random.nextInt(0, 32)}.0")!!
                else -> Ip.parse("10.${random.nextInt(0, 10)}.${random.nextInt(0, 32)}.0")!!
            }
            else -> when (random.nextInt(3)) {
                0 -> Ip.parse("10.${random.nextInt(0, 60)}.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}")!!
                1 -> Ip.parse("172.${random.nextInt(16, 32)}.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}")!!
                else -> Ip.parse("192.168.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}")!!
            }
        }
        return Ip.networkOf(raw, prefix)
    }
}

// ---------------------------------------------------------------------------
// Answers and grading
// ---------------------------------------------------------------------------

enum class FieldState { Empty, Correct, Wrong }

enum class VlsmField { Network, Prefix, FirstHost, LastHost, Broadcast }

enum class AnalyzeField { Network, Mask, FirstHost, LastHost, Broadcast, Hosts }

data class SubnetAnswer(
    val network: String = "",
    val prefix: String = "",
    val firstHost: String = "",
    val lastHost: String = "",
    val broadcast: String = "",
) {
    operator fun get(field: VlsmField): String = when (field) {
        VlsmField.Network -> network
        VlsmField.Prefix -> prefix
        VlsmField.FirstHost -> firstHost
        VlsmField.LastHost -> lastHost
        VlsmField.Broadcast -> broadcast
    }

    fun with(field: VlsmField, value: String): SubnetAnswer = when (field) {
        VlsmField.Network -> copy(network = value)
        VlsmField.Prefix -> copy(prefix = value)
        VlsmField.FirstHost -> copy(firstHost = value)
        VlsmField.LastHost -> copy(lastHost = value)
        VlsmField.Broadcast -> copy(broadcast = value)
    }

    val isBlank: Boolean
        get() = VlsmField.entries.all { this[it].isBlank() }
}

data class AnalyzeAnswer(
    val network: String = "",
    val mask: String = "",
    val firstHost: String = "",
    val lastHost: String = "",
    val broadcast: String = "",
    val hosts: String = "",
) {
    operator fun get(field: AnalyzeField): String = when (field) {
        AnalyzeField.Network -> network
        AnalyzeField.Mask -> mask
        AnalyzeField.FirstHost -> firstHost
        AnalyzeField.LastHost -> lastHost
        AnalyzeField.Broadcast -> broadcast
        AnalyzeField.Hosts -> hosts
    }

    fun with(field: AnalyzeField, value: String): AnalyzeAnswer = when (field) {
        AnalyzeField.Network -> copy(network = value)
        AnalyzeField.Mask -> copy(mask = value)
        AnalyzeField.FirstHost -> copy(firstHost = value)
        AnalyzeField.LastHost -> copy(lastHost = value)
        AnalyzeField.Broadcast -> copy(broadcast = value)
        AnalyzeField.Hosts -> copy(hosts = value)
    }

    val isBlank: Boolean
        get() = AnalyzeField.entries.all { this[it].isBlank() }
}

object Grader {

    private fun addressField(entered: String, expected: Long): FieldState = when {
        entered.isBlank() -> FieldState.Empty
        Ip.parse(entered) == expected -> FieldState.Correct
        else -> FieldState.Wrong
    }

    /**
     * `/26`, `26` and `255.255.255.192` are all marked right.
     *
     * They are three notations for one fact, and a learner who typed the dotted
     * mask has demonstrably done the harder half of the work. Marking that wrong
     * would be testing typing conventions, not subnetting.
     */
    private fun prefixField(entered: String, expected: Int): FieldState = when {
        entered.isBlank() -> FieldState.Empty
        Ip.parsePrefix(entered) == expected -> FieldState.Correct
        else -> FieldState.Wrong
    }

    private fun numberField(entered: String, expected: Long): FieldState {
        val cleaned = entered.filterNot { it == ' ' || it == ',' || it == ' ' }
        return when {
            cleaned.isBlank() -> FieldState.Empty
            cleaned.toLongOrNull() == expected -> FieldState.Correct
            else -> FieldState.Wrong
        }
    }

    fun grade(answer: SubnetAnswer, alloc: Allocation): Map<VlsmField, FieldState> = mapOf(
        VlsmField.Network to addressField(answer.network, alloc.network),
        VlsmField.Prefix to prefixField(answer.prefix, alloc.prefix),
        VlsmField.FirstHost to addressField(answer.firstHost, alloc.firstHost),
        VlsmField.LastHost to addressField(answer.lastHost, alloc.lastHost),
        VlsmField.Broadcast to addressField(answer.broadcast, alloc.broadcast),
    )

    fun grade(answer: AnalyzeAnswer, task: AnalyzeTask): Map<AnalyzeField, FieldState> = mapOf(
        AnalyzeField.Network to addressField(answer.network, task.network),
        AnalyzeField.Mask to addressField(answer.mask, task.mask),
        AnalyzeField.FirstHost to addressField(answer.firstHost, task.firstHost ?: task.network),
        AnalyzeField.LastHost to addressField(answer.lastHost, task.lastHost ?: task.broadcast),
        AnalyzeField.Broadcast to addressField(answer.broadcast, task.broadcast),
        AnalyzeField.Hosts to numberField(answer.hosts, task.usable),
    )

    fun expected(field: VlsmField, alloc: Allocation): String = when (field) {
        VlsmField.Network -> Ip.format(alloc.network)
        VlsmField.Prefix -> "/${alloc.prefix}"
        VlsmField.FirstHost -> Ip.format(alloc.firstHost)
        VlsmField.LastHost -> Ip.format(alloc.lastHost)
        VlsmField.Broadcast -> Ip.format(alloc.broadcast)
    }

    fun expected(field: AnalyzeField, task: AnalyzeTask): String = when (field) {
        AnalyzeField.Network -> Ip.format(task.network)
        AnalyzeField.Mask -> Ip.format(task.mask)
        AnalyzeField.FirstHost -> task.firstHost?.let { Ip.format(it) } ?: "—"
        AnalyzeField.LastHost -> task.lastHost?.let { Ip.format(it) } ?: "—"
        AnalyzeField.Broadcast -> Ip.format(task.broadcast)
        AnalyzeField.Hosts -> task.usable.toString()
    }
}
