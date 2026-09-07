package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStage
import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.VlsmStage
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

    override val appName = "NetLab"
    override val tabLearn = "Learn"
    override val tabExercise = "Exercise"
    override val tabCalculator = "Calculator"
    override val tabSettings = "Settings"

    // ---- shared vocabulary ---------------------------------------------------

    override val actionCheck = "Check"
    override val actionSolution = "Full solution"
    override val actionNewExercise = "New exercise"
    override val actionNext = "Next"
    override val actionBack = "Back"
    override val actionClose = "Close"
    override val actionAdd = "Add"
    override val actionShowAll = "All steps"
    override val actionOneByOne = "One at a time"
    override val actionClear = "Clear"
    override val actionConfirm = "Reset"
    override val actionCancel = "Cancel"
    override val actionContinue = "Continue"
    override val actionRetry = "Try again"
    override val actionHint = "How do I do this?"
    override val actionHideHint = "Hide"
    override val actionShowAnswer = "Show me the answer"
    override val actionReveal = "Show answer"

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

    // ---- exercise ------------------------------------------------------------

    override val exerciseTitle = "Exercise"
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

    override fun subnetTitle(name: String) = "Subnet $name"
    override fun stageOf(index: Int, total: Int) = "Step $index of $total"

    // ---- exercise stages -----------------------------------------------------

    override fun vlsmStageTitle(stage: VlsmStage) = when (stage) {
        VlsmStage.Order -> "Put them in order"
        VlsmStage.Size -> "How big is each one?"
        VlsmStage.Place -> "Where does each block go?"
        VlsmStage.Done -> "Finished"
    }

    override fun vlsmStagePrompt(stage: VlsmStage) = when (stage) {
        VlsmStage.Order -> "Drag the subnets so the one that needs the most hosts is at the top."
        VlsmStage.Size -> "For each subnet work out the prefix. Hosts + 2, round up to a power of two, prefix = 32 − the exponent."
        VlsmStage.Place -> "Start at the first free address and read each block off: network, first host, last host, broadcast."
        VlsmStage.Done -> "Every stage is answered."
    }

    override fun vlsmStageHint(stage: VlsmStage) = when (stage) {
        VlsmStage.Order -> listOf(
            "A block of 2^n addresses may only begin at an address that is a multiple of 2^n. A block of 64 starts at 0, 64, 128 or 192 — never at 16.",
            "Hand out a small subnet first and the next big one has nowhere aligned to land, so it has to skip forward and everything skipped over is stranded.",
            "Going largest first, the next free address is always already aligned for whatever comes next.",
        )
        VlsmStage.Size -> listOf(
            "Add 2 to the host count first: one address goes to the network address, one to the broadcast, and neither can be given to a machine.",
            "Then climb the ladder 2, 4, 8, 16, 32, 64, 128… and stop at the first rung that is big enough. Round up, never to the nearest.",
            "That exponent is the host bits h. The prefix is 32 − h. So 60 hosts → 62 addresses → 2^6 = 64 → /26.",
        )
        VlsmStage.Place -> listOf(
            "The first subnet starts at the base network's own address. Each one after it starts at the previous broadcast + 1.",
            "Broadcast = network + block size − 1. The −1 is because the network address itself is the first of the block's addresses.",
            "First host = network + 1, last host = broadcast − 1.",
        )
        VlsmStage.Done -> emptyList()
    }

    override fun analyzeStageTitle(stage: AnalyzeStage) = when (stage) {
        AnalyzeStage.Mask -> "The mask and the block size"
        AnalyzeStage.Network -> "The network address"
        AnalyzeStage.Broadcast -> "The broadcast address"
        AnalyzeStage.Hosts -> "The hosts"
        AnalyzeStage.Done -> "Finished"
    }

    override fun analyzeStagePrompt(stage: AnalyzeStage) = when (stage) {
        AnalyzeStage.Mask -> "Write the prefix out as a dotted mask, and say how many addresses the block holds."
        AnalyzeStage.Network -> "Round the given address down to the start of its block."
        AnalyzeStage.Broadcast -> "The last address of that same block."
        AnalyzeStage.Hosts -> "The two ends of the usable range, and how many addresses fit between them."
        AnalyzeStage.Done -> "Every stage is answered."
    }

    override fun analyzeStageHint(stage: AnalyzeStage) = when (stage) {
        AnalyzeStage.Mask -> listOf(
            "The prefix is how many ones the mask starts with. Split it into octets: /26 is 8 + 8 + 8 + 2.",
            "An octet of a mask can only be 0, 128, 192, 224, 240, 248, 252, 254 or 255 — one value per number of ones. Two ones is 192.",
            "Block size = 2^h, where h = 32 − prefix. A /26 leaves 6 host bits, so 2^6 = 64 addresses.",
        )
        AnalyzeStage.Network -> listOf(
            "Find the interesting octet — the one where the mask is neither 255 nor 0.",
            "Magic number = 256 − that mask octet. Blocks begin at every multiple of it.",
            "Round the address's value in that octet down to a multiple of the magic number, and set every octet to the right of it to 0.",
        )
        AnalyzeStage.Broadcast -> listOf(
            "Broadcast = network + block size − 1.",
            "By hand it is quicker in the interesting octet: add the magic number, subtract 1, and set every octet to the right to 255.",
        )
        AnalyzeStage.Hosts -> listOf(
            "First host = network address + 1. The network address itself can never be given to a machine.",
            "Last host = broadcast − 1, for the same reason at the other end.",
            "Usable = 2^h − 2. Those are the two addresses you just skipped.",
        )
        AnalyzeStage.Done -> emptyList()
    }

    override val hintTitle = "The rule"
    override val dragHandle = "Drag to reorder"
    override val orderPrompt = "Largest at the top"
    override val orderTopLabel = "Placed first"
    override val orderBottomLabel = "Placed last"

    override val stageCorrect = "Correct."
    override val stageWrongOrder = "Not in order yet. The subnet needing the most hosts goes at the top."
    override fun stageWrongFields(wrong: Int) =
        "$wrong ${if (wrong == 1) "answer is" else "answers are"} wrong."
    override val stageIncomplete = "Fill everything in first."
    override val stageRevealed = "Answer shown. Read it, then continue."

    override val resultTitle = "Exercise complete"
    override val resultPerfect = "Solved with no mistakes."
    override fun resultMistakes(count: Int) =
        "Solved, with $count ${if (count == 1) "stage" else "stages"} that needed a second go."
    override val resultPlan = "Your plan"
    override val resultFree = "left over"

    // ---- walkthrough ---------------------------------------------------------

    override val solutionTitle = "Step by step"
    override val ruleLabel = "The rule"
    override fun stepOf(index: Int, total: Int) = "Step $index of $total"

    override val stepIntroTitle = "What you were given"
    override val stepIntroRule = "Capacity of the base network = 2^(32 − prefix). Cost of the assignment = every subnet rounded up to a power of two, added together."

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
    override val stepOrderRule = "Always allocate largest first. A block of 2^n may only start at a multiple of 2^n, and descending order keeps the cursor aligned for free."

    override fun stepOrderBody(step: VlsmStep.Order): List<String> = listOf(
        "Order the subnets by size, biggest first: ${step.ordered.joinToString(", ") { "${it.name} (${num(it.hosts.toLong())})" }}.",
        "This is the method, not housekeeping. A block of 2^n addresses may only begin at an address that is a multiple of 2^n — a /26 at 0, 64, 128 or 192, never at 16.",
        "Hand out a small subnet first and the next big one has no aligned place to start, so you have to skip forward and everything you skipped is lost. Going largest first, the next free address is already aligned for whatever comes next, every time.",
    )

    override val stepSizeRule = "hosts + 2 → round UP to the next power of two 2^h → prefix = 32 − h."

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

    override val stepPlaceRule = "Network = the first free address (already aligned). Broadcast = network + block size − 1. First = network + 1, last = broadcast − 1."

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

    override val stepOverflowRule = "A block cannot straddle the end of the base network. If what is left is smaller than the block, the subnet cannot be placed at all."

    override fun stepOverflowTitle(step: VlsmStep.Overflow) =
        "${subnetTitle(step.requirement.name)} does not fit"

    override fun stepOverflowBody(step: VlsmStep.Overflow): List<String> = listOf(
        "${step.requirement.name} asks for ${hosts(step.requirement.hosts.toLong())}, which rounds up to a block of ${addresses(step.needed)}.",
        "Only ${addresses(step.remaining)} are left in the base network, and a block cannot be split across the end of it.",
        "Either the base network has to be larger, or this subnet has to ask for less.",
    )

    override val stepVerifyTitle = "Check the plan"
    override val stepVerifyRule = "Every network address is a multiple of its own block size, and each broadcast is exactly one below the next network address."

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

    override fun analyzeStepRule(step: AnalyzeStep): String = when (step) {
        is AnalyzeStep.Mask -> "prefix ones, then zeros to 32, cut into four octets. An octet of a mask is only ever 0, 128, 192, 224, 240, 248, 252, 254 or 255."
        is AnalyzeStep.Magic -> "magic number = 256 − the mask octet that is neither 255 nor 0. Blocks begin at every multiple of it."
        is AnalyzeStep.Network -> "Round the interesting octet down to a multiple of the magic number; zero every octet to its right."
        is AnalyzeStep.Broadcast -> "broadcast = network + block size − 1; to the right of the interesting octet, everything is 255."
        is AnalyzeStep.Hosts -> "first = network + 1, last = broadcast − 1."
        is AnalyzeStep.Count -> "usable = 2^h − 2, where h = 32 − prefix."
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
    override val designerTypeHosts = "Type the exact number of hosts, or drag the slider."

    // ---- learn ---------------------------------------------------------------

    override val learnTitle = "Learn"
    override val learnIntro = "Nine chapters, from \"what is an IP address\" to a full VLSM plan. Everything is done with pen, paper and your head — no calculator anywhere."
    override val learnStartHere = "Start here"
    override val course = enCourse
    override fun chapterOf(index: Int, total: Int) = "Chapter $index of $total"
    override fun pageOf(index: Int, total: Int) = "Page $index of $total"
    override val chapterRead = "Read"
    override val chapterDone = "Chapter finished"
    override val chapterDoneBody = "Try the next one, or go to Exercise and do one for real."
    override val checkYourself = "Check yourself"

    // ---- settings ------------------------------------------------------------

    override val settingsTitle = "Settings"
    override val settingLanguage = "Language"
    override val settingTheme = "Appearance"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val settingDefaults = "Exercise"
    override val settingDefaultKind = "Exercise type"
    override val settingDefaultDifficulty = "Difficulty"
    override val progressTitle = "Progress"
    override val progressChapters = "Chapters read"
    override val progressExercises = "Exercises solved"
    override val progressClean = "Solved with no help"
    override val progressNone = "Nothing yet — read a chapter or solve an exercise."
    override val settingStats = "Statistics"
    override val settingResetStats = "Reset statistics"
    override val settingResetStatsHint = "Clears the solved count and both streaks."
    override val settingAbout = "About"
    override val aboutBody = "NetLab — IPv4 subnetting and VLSM practice. Every number is worked out on the device and the app works with no signal at all; the network is used only to sign in and to keep your progress in step with netlab.n3d-store.com."
    override val languageEnglish = "English"
    override val languageCzech = "Čeština"

    override val accountTitle = "Account"
    override val accountGuest = "Not signed in"
    override val accountGuestBody =
        "Progress is kept on this phone only. Sign in and it follows you to any device."
    override val accountSignedInAs = "Signed in as"
    override val actionSignIn = "Sign in"
    override val actionRegister = "Create account"
    override val actionSignOut = "Sign out"
    override val fieldEmail = "Email"
    override val fieldPassword = "Password"
    override val fieldName = "Display name"
    override val fieldNameHint = "Optional — shown on your account."
    override val passwordHint = "At least 8 characters."
    override val authHaveAccount = "Already have an account? Sign in"
    override val authNoAccount = "No account yet? Create one"
    override val authWorking = "Working…"
    override val authSyncedIn = "Signed in. Your progress from this phone has been merged in."
    override val authSignedOut = "Signed out. Progress stays on this phone."

    override val mfaTitle = "Check your email"
    override fun mfaBody(email: String) =
        "We sent a six-digit code to $email. It expires in 10 minutes."
    override val mfaWhy =
        "Every sign-in needs the code, so knowing your password is not enough to get into your account."
    override val fieldCode = "Code"
    override val actionVerify = "Sign in"
    override val actionResend = "Send a new code"
    override val actionUseAnother = "Use a different email"
    override val mfaResent = "A new code is on its way."
    override val mfaSpam = "Nothing yet? Check the spam folder — it comes from the Nebula 3D mailbox."

    override val forgotPassword = "Forgot your password?"
    override val resetTitle = "Reset your password"
    override val resetBody = "Type the address on your account and we will email you a link."
    override val actionSendReset = "Email me a link"
    override val actionBackToSignIn = "Back to sign in"
    override fun resetSent(email: String) =
        "If $email has an account, a link is on its way. Open it in your browser — it works once and expires in an hour."

    override fun authError(code: String) = when (code) {
        "email" -> "That does not look like an email address."
        "password" -> "The password needs at least 8 characters."
        "taken" -> "There is already an account with that email."
        "credentials" -> "Wrong email or password."
        "rate" -> "Too many attempts. Wait a few minutes and try again."
        "code" -> "That code is not right. Check the email and try again."
        "expired" -> "That code has expired. Ask for a new one."
        "attempts" -> "Too many wrong codes. Start again and we will send a new one."
        "mail" -> "We could not send the email just now. Try again in a minute."
        "network" -> "Could not reach the server. Try again."
        else -> "Something went wrong. Try again."
    }
}
