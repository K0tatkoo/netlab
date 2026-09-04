package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.VlsmStep

object En : Strings {

    override val lang = Lang.En

    // ---- formatting ----------------------------------------------------------

    override fun num(value: Long): String {
        val s = value.toString()
        if (s.length <= 4) return s
        return s.reversed().chunked(3).joinToString(",").reversed()
    }

    override fun hosts(count: Long) = "${num(count)} ${if (count == 1L) "host" else "hosts"}"
    override fun addresses(count: Long) = "${num(count)} ${if (count == 1L) "address" else "addresses"}"
    override fun subnets(count: Int) = "$count ${if (count == 1) "subnet" else "subnets"}"
    override fun bits(count: Int) = "$count ${if (count == 1) "bit" else "bits"}"

    private fun ordinal(n: Int) = when (n) {
        1 -> "first"; 2 -> "second"; 3 -> "third"; else -> "fourth"
    }

    // ---- navigation ----------------------------------------------------------

    override val tabPractice = "Practice"
    override val tabCalculator = "Calculator"
    override val tabLearn = "Learn"
    override val tabSettings = "Settings"

    // ---- shared vocabulary ---------------------------------------------------

    override val actionCheck = "Check"
    override val actionSolution = "Step by step"
    override val actionHideSolution = "Hide solution"
    override val actionNewExercise = "New exercise"
    override val actionNext = "Next"
    override val actionBack = "Back"
    override val actionReset = "Reset"
    override val actionClose = "Close"
    override val actionAdd = "Add"
    override val actionShowAll = "All steps"
    override val actionOneByOne = "One at a time"
    override val actionFillCorrect = "Fill in answers"
    override val actionClear = "Clear"
    override val actionConfirm = "Reset"
    override val actionCancel = "Cancel"

    override val labelCorrect = "Correct"
    override val labelWrong = "Wrong"
    override val labelExpected = "Answer"
    override val labelSolved = "Solved"
    override val labelStreak = "Streak"
    override val labelBest = "Best"
    override val labelDifficulty = "Difficulty"
    override val diffEasy = "Easy"
    override val diffMedium = "Medium"
    override val diffHard = "Hard"

    override val fieldNetwork = "Network address"
    override val fieldPrefix = "Prefix"
    override val fieldMask = "Subnet mask"
    override val fieldFirstHost = "First host"
    override val fieldLastHost = "Last host"
    override val fieldBroadcast = "Broadcast"
    override val fieldUsableHosts = "Usable hosts"
    override val fieldWildcard = "Wildcard mask"
    override val fieldBlockSize = "Block size"
    override val fieldHostBits = "Host bits"
    override val fieldNetworkBits = "Network bits"
    override val fieldRange = "Range"
    override val fieldTotalAddresses = "Total addresses"
    override val fieldClass = "Class"
    override val fieldScope = "Scope"
    override val fieldBinary = "Binary"
    override val fieldSpare = "Spare"
    override val fieldAddress = "Address"

    override val classA = "Class A"
    override val classB = "Class B"
    override val classC = "Class C"
    override val classD = "Class D (multicast)"
    override val classE = "Class E (reserved)"
    override val classLoopback = "Loopback"
    override val classThisNetwork = "This network"

    override val scopePrivate = "Private (RFC 1918)"
    override val scopePublic = "Public"
    override val scopeLoopback = "Loopback"
    override val scopeLinkLocal = "Link-local"
    override val scopeCgnat = "Carrier-grade NAT"
    override val scopeMulticast = "Multicast"
    override val scopeReserved = "Reserved"

    // ---- practice ------------------------------------------------------------

    override val practiceTitle = "Practice"
    override val kindVlsm = "Split a network"
    override val kindAnalyze = "Analyse an address"
    override val kindVlsmHint = "Carve one network into subnets of different sizes"
    override val kindAnalyzeHint = "One address and a prefix — find everything else"

    override val assignment = "Assignment"
    override val baseNetwork = "Base network"
    override val givenAddress = "Given"
    override val vlsmPrompt = "Split the base network so every subnet below gets at least the hosts it asks for, and waste as little as possible."
    override val analyzePrompt = "Work out everything about the network this address belongs to."
    override val yourAnswer = "Your answer"

    override fun requirement(name: String, hostCount: Int) = "$name — ${hosts(hostCount.toLong())}"
    override fun subnetTitle(name: String) = "Subnet $name"

    override val verdictPerfect = "All correct."
    override fun verdictWrong(wrong: Int, total: Int) =
        "$wrong of $total ${if (total == 1) "field is" else "fields are"} wrong."
    override val verdictIncomplete = "Some fields are still empty."
    override val verdictNothing = "Fill something in first."
    override val notCheckedYet = "Not checked yet"

    // ---- walkthrough ---------------------------------------------------------

    override val solutionTitle = "Step by step"
    override fun stepOf(index: Int, total: Int) = "Step $index of $total"

    override val stepIntroTitle = "What you were given"

    override fun stepIntroBody(step: VlsmStep.Intro): List<String> {
        val last = step.base + step.capacity - 1
        val spare = step.capacity - step.demand
        val verdict = if (step.demand <= step.capacity) {
            // "fits, with 0 addresses left over" reads as a bug rather than as
            // the perfectly packed plan it actually is.
            if (spare == 0L) {
                "${num(step.demand)} = ${num(step.capacity)}, so the assignment fits exactly, with nothing left over."
            } else {
                "${num(step.demand)} ≤ ${num(step.capacity)}, so the assignment fits, and ${addresses(spare)} will be left over."
            }
        } else {
            "${num(step.demand)} > ${num(step.capacity)}, so the assignment does not fit. Something will have to be left out."
        }
        return listOf(
            "The base network is ${Ip.cidr(step.base, step.basePrefix)}. Its mask is ${Ip.format(Ip.mask(step.basePrefix))}, and it covers ${addresses(step.capacity)} — from ${Ip.format(step.base)} to ${Ip.format(last)}.",
            "The very first of those is the network address and the very last is the broadcast address, so ${num(step.usable)} of them could carry a host.",
            "You have to fit ${subnets(step.subnetCount)} inside it. Between them they ask for ${hosts(step.requestedHosts.toLong())} — but each subnet also has to pay for its own network and broadcast address and then round up to a power of two, so the real cost is ${addresses(step.demand)}.",
            verdict,
        )
    }

    override val stepOrderTitle = "Sort them, largest first"

    override fun stepOrderBody(step: VlsmStep.Order): List<String> = listOf(
        "Order the subnets by size, biggest first: ${step.ordered.joinToString(", ") { "${it.name} (${num(it.hosts.toLong())})" }}.",
        "This is the method, not housekeeping. A block of 2^n addresses may only begin at an address that is a multiple of 2^n — a /26 at 0, 64, 128 or 192, never at 16.",
        "Hand out a small subnet first and the next big one has no aligned place to start, so you have to skip forward and everything you skipped is lost. Going largest first, the next free address is already aligned for whatever comes next, every time.",
    )

    override fun stepSizeTitle(step: VlsmStep.Size) = "${subnetTitle(step.alloc.requirement.name)} — how big?"

    override fun stepSizeBody(step: VlsmStep.Size): List<String> {
        val a = step.alloc
        val needed = a.requirement.hosts + 2
        val smaller = 1L shl (a.hostBits - 1)
        return listOf(
            "${a.requirement.name} asks for ${hosts(a.requirement.hosts.toLong())}.",
            "Add two: one address goes to the network address and one to the broadcast address, and neither can be given to a machine. So the subnet has to hold ${a.requirement.hosts} + 2 = $needed addresses.",
            "Now take the smallest power of two that is at least $needed. 2^${a.hostBits - 1} = ${num(smaller)} is too small; 2^${a.hostBits} = ${num(a.blockSize)} is enough. That is ${bits(a.hostBits)} of host space.",
            "The prefix is whatever is left over: 32 − ${a.hostBits} = /${a.prefix}, mask ${Ip.format(a.mask)}. The block is ${addresses(a.blockSize)} wide, ${num(a.usable)} of them usable — ${num(a.spare)} to spare.",
        )
    }

    override fun stepPlaceTitle(step: VlsmStep.Place) = "${subnetTitle(step.alloc.requirement.name)} — where?"

    override fun stepPlaceBody(step: VlsmStep.Place): List<String> {
        val a = step.alloc
        val (octetIndex, magic) = Ip.magicNumber(a.prefix)
        val octetValue = Ip.octet(a.network, octetIndex - 1)
        val boundaries = generateSequence(0L) { it + magic }.takeWhile { it < 256 }.toList()
        val boundaryText = if (boundaries.size <= 5) {
            boundaries.joinToString(", ")
        } else {
            boundaries.take(4).joinToString(", ") + ", …"
        }
        return listOf(
            "The first free address is ${Ip.format(step.cursorBefore)}.",
            "A /${a.prefix} is ${addresses(a.blockSize)} wide, so it may only begin where the ${ordinal(octetIndex)} octet is a multiple of ${num(magic)} — $boundaryText — with every octet after it zero. The first free address already satisfies that, because you are working from the largest subnet down.",
            "So ${a.requirement.name} is ${Ip.cidr(a.network, a.prefix)}, its ${ordinal(octetIndex)} octet being $octetValue.",
            "The broadcast address is the last one in the block: ${Ip.format(a.network)} + ${num(a.blockSize)} − 1 = ${Ip.format(a.broadcast)}. Everything between the two ends is usable, so hosts run ${Ip.format(a.firstHost)} – ${Ip.format(a.lastHost)}.",
            "The next free address is ${Ip.format(step.cursorAfter)}. That is where the next subnet begins.",
        )
    }

    override fun stepOverflowTitle(step: VlsmStep.Overflow) =
        "${subnetTitle(step.requirement.name)} does not fit"

    override fun stepOverflowBody(step: VlsmStep.Overflow): List<String> = listOf(
        "${step.requirement.name} asks for ${hosts(step.requirement.hosts.toLong())}, which rounds up to a block of ${addresses(step.needed)}.",
        "Only ${addresses(step.remaining)} are left in the base network, and a block cannot be split across the end of it.",
        "Either the base network has to be larger, or this subnet has to ask for less.",
    )

    override val stepVerifyTitle = "Check the plan"

    override fun stepVerifyBody(step: VlsmStep.Verify): List<String> {
        val p = step.plan
        val lines = mutableListOf(
            "Add the blocks up: ${num(p.used)} of ${num(p.capacity)} addresses are handed out.",
        )
        lines += if (p.free > 0) {
            "That leaves ${addresses(p.free)} unused at the top of the range, ${Ip.format(p.nextFree)} – ${Ip.format(p.endExclusive - 1)}. That is where a fifth subnet, or a later expansion, would go."
        } else {
            "Nothing is left over — the base network is used to the last address."
        }
        lines += "Inside the subnets, ${num(p.wasted)} usable addresses are not claimed by anyone. That is the price of rounding every subnet up to a power of two, and it is normal."
        lines += "Two things make the plan correct, and both are worth checking by hand: every network address is a multiple of its own block size, and no two ranges overlap. Reading down the list, each subnet's broadcast address is exactly one less than the next subnet's network address."
        return lines
    }

    override fun analyzeStepTitle(step: AnalyzeStep): String = when (step) {
        is AnalyzeStep.Mask -> "Write the mask out"
        is AnalyzeStep.Magic -> "Find the magic number"
        is AnalyzeStep.Network -> "Round down to the network address"
        is AnalyzeStep.Broadcast -> "Find the broadcast address"
        is AnalyzeStep.Hosts -> "The first and last host"
        is AnalyzeStep.Count -> "How many hosts fit"
    }

    override fun analyzeStepBody(step: AnalyzeStep): List<String> = when (step) {
        is AnalyzeStep.Mask -> {
            val t = step.task
            listOf(
                "A /${t.prefix} means the first ${bits(t.prefix)} of the address identify the network and the remaining ${bits(t.hostBits)} identify the host inside it.",
                "So write ${t.prefix} ones followed by ${t.hostBits} zeros, and cut the 32 bits into four octets of eight: ${Ip.toBinary(t.mask)}.",
                "Convert each octet back to decimal and the mask is ${Ip.format(t.mask)}.",
            )
        }

        is AnalyzeStep.Magic -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val maskOctet = Ip.octet(t.mask, octetIndex - 1)
            if (t.hostBits % 8 == 0) {
                listOf(
                    "/${t.prefix} lands exactly on an octet boundary, so every octet of the mask is either 255 or 0 and there is no partial octet to work with.",
                    "That makes it the easy case: blocks step by 1 in the ${ordinal(octetIndex)} octet, and every octet after it runs the full 0–255.",
                )
            } else {
                listOf(
                    "Look for the octet where the mask is neither 255 nor 0 — the interesting octet. Here it is the ${ordinal(octetIndex)}, holding $maskOctet.",
                    "The magic number is 256 − $maskOctet = ${num(magic)}. It is the block size measured in that octet, and blocks can only begin at its multiples: ${generateSequence(0L) { it + magic }.takeWhile { it < 256 }.take(5).joinToString(", ")}${if (256 / magic > 5) ", …" else ""}.",
                    "Everything after the interesting octet is host space and runs the full 0–255.",
                )
            }
        }

        is AnalyzeStep.Network -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val octetValue = Ip.octet(t.address, octetIndex - 1)
            listOf(
                "The address is ${Ip.format(t.address)}. Its ${ordinal(octetIndex)} octet is $octetValue.",
                "Round that down to the nearest multiple of ${num(magic)}: $octetValue ÷ ${num(magic)} = ${octetValue / magic} remainder ${octetValue % magic}, so ${octetValue / magic} × ${num(magic)} = ${num(step.multiple)}. Set every octet after it to 0.",
                "Network address = ${Ip.format(t.network)}.",
                "The same result the long way, if you want to see why: line the address up against the mask and AND them bit by bit — a bit survives only where the mask has a 1.",
            )
        }

        is AnalyzeStep.Broadcast -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val netOctet = Ip.octet(t.network, octetIndex - 1)
            listOf(
                "The broadcast address is simply the last address in the block: network + ${num(t.blockSize)} − 1.",
                "The shortcut is faster by hand: add ${num(magic)} − 1 = ${num(magic - 1)} to the ${ordinal(octetIndex)} octet, giving ${netOctet + magic - 1}, and set every octet after it to 255.",
                "Broadcast = ${Ip.format(t.broadcast)}.",
            )
        }

        is AnalyzeStep.Hosts -> {
            val t = step.task
            if (t.prefix >= 31) {
                listOf(
                    "A /${t.prefix} has no usable host range in the classic scheme: /31 holds only a network and a broadcast address, and /32 is a single address.",
                    "In practice /31 is used on point-to-point links, where RFC 3021 lets both addresses carry a router. Exams almost always want the classic answer of zero.",
                )
            } else {
                listOf(
                    "The network address itself cannot be given to a machine, so the first host is one above it: ${Ip.format(t.network)} + 1 = ${Ip.format(t.firstHost!!)}.",
                    "The broadcast address cannot either, so the last host is one below it: ${Ip.format(t.broadcast)} − 1 = ${Ip.format(t.lastHost!!)}.",
                    "Usable range: ${Ip.format(t.firstHost!!)} – ${Ip.format(t.lastHost!!)}.",
                )
            }
        }

        is AnalyzeStep.Count -> {
            val t = step.task
            listOf(
                "There are ${bits(t.hostBits)} of host space, so the block holds 2^${t.hostBits} = ${addresses(t.blockSize)}.",
                "Take away the network address and the broadcast address: ${num(t.blockSize)} − 2 = ${num(t.usable)} usable hosts.",
                "Sanity check the two ends against each other — counting from ${Ip.format(t.firstHost ?: t.network)} to ${Ip.format(t.lastHost ?: t.broadcast)} gives the same ${num(t.usable)}.",
            )
        }
    }

    override fun bitsCaption(networkBits: Int) = "first ${bits(networkBits)} = network, rest = host"
    override val bitsLegendNetwork = "network"
    override val bitsLegendHost = "host"

    // ---- rows ----------------------------------------------------------------

    override val rowSubnetsRequired = "Subnets required"
    override val rowHostsRequested = "Hosts requested"
    override val rowAddressesNeeded = "Addresses needed"
    override val rowHostsRequired = "Hosts required"
    override val rowPlusTwo = "+ network & broadcast"
    override val rowFirstFree = "First free address"
    override val rowNextFree = "Next free address"
    override val rowUsed = "Allocated"
    override val rowFree = "Left over"
    override val rowWasted = "Unused inside subnets"
    override val rowMagicOctet = "Interesting octet"
    override val rowMagicNumber = "Magic number"
    override val rowRoundedDown = "Rounded down to"

    // ---- calculator ----------------------------------------------------------

    override val calculatorTitle = "Calculator"
    override val calcModeSubnet = "Subnet"
    override val calcModeDesigner = "VLSM plan"
    override val calcInvalidAddress = "Not a valid IPv4 address"
    override val calcEnterAddress = "Enter an address to see the numbers"
    override val designerBase = "Base network"
    override val designerSubnets = "Subnets"
    override val designerAddSubnet = "Add subnet"
    override val designerRemove = "Remove"
    override val designerEmpty = "Add a subnet to build a plan."
    override fun designerFits(used: Long, capacity: Long) =
        "${num(used)} of ${num(capacity)} addresses used"
    override fun designerOverflow(count: Int) =
        "$count ${if (count == 1) "subnet does" else "subnets do"} not fit"
    override val designerPlan = "Plan"
    override val designerHostsFor = "Hosts"

    // ---- learn ---------------------------------------------------------------

    override val learnTitle = "Learn"
    override val lessons = enLessons

    // ---- settings ------------------------------------------------------------

    override val settingsTitle = "Settings"
    override val settingLanguage = "Language"
    override val settingTheme = "Appearance"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val settingDefaults = "Practice"
    override val settingDefaultKind = "Exercise type"
    override val settingDefaultDifficulty = "Difficulty"
    override val settingShortFields = "Short answers"
    override val settingShortFieldsHint = "Ask only for the network address and the prefix, not the whole range."
    override val settingStats = "Statistics"
    override val settingResetStats = "Reset statistics"
    override val settingResetStatsHint = "Clears the solved count and both streaks."
    override val settingAbout = "About"
    override val aboutBody = "NetLab — IPv4 subnetting and VLSM practice. Everything is worked out on the device; the app has no network permission and sends nothing anywhere."
    override val languageEnglish = "English"
    override val languageCzech = "Čeština"
}
