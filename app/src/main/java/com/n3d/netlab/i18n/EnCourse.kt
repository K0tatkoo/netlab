package com.n3d.netlab.i18n

import com.n3d.netlab.core.Ip

/**
 * The English course.
 *
 * Written for somebody who has never seen a subnet mask. Chapters 1–4 build the
 * vocabulary, chapter 5 is the procedure the whole app is about, and chapters
 * 7–9 apply it. Every worked example prints every operation, including the ones
 * that feel too small to write down — those are exactly the ones that get
 * skipped and then get the answer wrong.
 */
internal val enCourse: List<Chapter> = listOf(

    // -----------------------------------------------------------------------
    Chapter(
        title = "What are we even doing?",
        summary = "Start here — no prior knowledge assumed",
        pages = listOf(
            Page(
                "An address, and the network it lives in",
                listOf(
                    Block.Para("Every device on a network has an IPv4 address: four numbers, each 0 to 255, separated by dots."),
                    Block.Formula("192.168.1.10"),
                    Block.Para("On its own that address does not say much. What matters in practice is which group of addresses it belongs to — its network. Devices in the same network talk to each other directly; to reach anything outside it they have to go through a router."),
                    Block.Para("So an address always comes with a second piece of information saying where its network starts and ends. That is written as a slash and a number:"),
                    Block.Formula("192.168.1.10 /24"),
                    Block.Para("Read it as: \"this address, in a network whose first 24 bits are fixed\". Everything in this app is about turning that one line into the full picture of the network."),
                ),
            ),
            Page(
                "The six answers you will always produce",
                listOf(
                    Block.Para("Whatever the question looks like, subnetting always ends in the same six facts about a block of addresses. Learn what each one means now and the rest is arithmetic."),
                    Block.Table(listOf(
                        "Subnet mask" to "the long form of /24 — 255.255.255.0",
                        "Network address" to "the first address; names the block",
                        "First host" to "network + 1",
                        "Last host" to "broadcast − 1",
                        "Broadcast" to "the last address; reaches everyone",
                        "Usable hosts" to "how many devices actually fit",
                    )),
                    Block.Split(
                        caption = "One block of 256 addresses, 192.168.1.0/24",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("net", 1, "192.168.1.0"),
                            SplitPart("hosts .1 – .254", 254, "254 usable"),
                            SplitPart("bc", 1, "192.168.1.255"),
                        ),
                    ),
                    Block.Para("The block is a run of consecutive addresses. The first one and the last one are reserved, and everything between them is yours to hand out."),
                ),
            ),
            Page(
                "Why anyone splits a network",
                listOf(
                    Block.Para("A company gets one block of addresses and has to run several separate networks inside it: an office, a warehouse, a wifi, a link between two routers. Each one needs its own block."),
                    Block.Para("Cutting one block into smaller ones is called subnetting. Cutting it into pieces of different sizes — a big one for the office, a tiny one for the router link — is called VLSM, variable-length subnet masking."),
                    Block.Split(
                        caption = "The same 256 addresses, split four ways",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("A · 64", 64, "60 hosts"),
                            SplitPart("B · 32", 32, "30 hosts"),
                            SplitPart("C · 16", 16, "14 hosts"),
                            SplitPart("D · 16", 16, "7 hosts"),
                            SplitPart("free · 128", 128, "still unused", free = true),
                        ),
                    ),
                    Block.Para("Those are the only two questions this app asks. Analyse one address, or split one network. The Exercise tab generates both."),
                    Block.Tip("You never need a calculator for any of it. Everything reduces to doubling, halving and subtracting from 256 — all of which fit in your head or in the margin of a page."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Binary, the small amount you need",
        summary = "Eight columns, and doubling",
        pages = listOf(
            Page(
                "An address is really one 32-bit number",
                listOf(
                    Block.Para("A computer stores the address as 32 bits — 32 ones and zeros in a row. The four dotted numbers are just those bits sliced into four groups of eight. A group of eight bits is called an octet."),
                    Block.Bits("192.168.1.10 as the computer holds it", Ip.bits(Ip.parse("192.168.1.10")!!), 24),
                    Block.Para("Eight bits is why no part can exceed 255: eight ones is the largest value there is. The dots are punctuation and never take part in the maths."),
                ),
            ),
            Page(
                "The eight columns",
                listOf(
                    Block.Para("Inside one octet, each bit is worth double the bit to its right. Learn this row and you can convert either direction:"),
                    Block.Formula("128  64  32  16  8  4  2  1"),
                    Block.Para("Binary to decimal: add up the columns that have a 1 in them."),
                    Block.PlaceValue("10101100", 172),
                    Block.Work("adding the columns", listOf(
                        "128 + 32 + 8 + 4  =  172",
                    )),
                    Block.Para("Decimal to binary: walk left to right and ask \"does it fit?\". If it does, write 1 and subtract; if not, write 0 and move on. Converting 200:"),
                    Block.Work("200 to binary", listOf(
                        "128 fits in 200  -> 1   200-128 =  72",
                        " 64 fits in  72  -> 1    72- 64 =   8",
                        " 32 too big      -> 0",
                        " 16 too big      -> 0",
                        "  8 fits in   8  -> 1     8-  8 =   0",
                        "  4 too big      -> 0",
                        "  2 too big      -> 0",
                        "  1 too big      -> 0",
                        "",
                        "200  =  11001000",
                    )),
                    Block.PlaceValue("200", 200),
                ),
            ),
            Page(
                "The powers of two",
                listOf(
                    Block.Para("Every block of addresses is a power of two — there is no such thing as a block of 100. This table is the single most useful thing to be able to write from memory, and you build it by doubling."),
                    Block.Table(powersTable(1..16)),
                    Block.Tip("On paper, write this ladder in the corner before you start. Every question is then answered by finding a number in it."),
                    Block.Check(
                        question = "Which power of two is the smallest one that is at least 300?",
                        answer = "2^9 = 512",
                        why = "256 is 2^8 and that is short of 300, so you go one more rung up the ladder. There is nothing between them.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "The mask and the prefix",
        summary = "Where the network ends and the hosts begin",
        pages = listOf(
            Page(
                "One line, drawn through 32 bits",
                listOf(
                    Block.Para("The mask splits the 32 bits into two parts: a network part on the left and a host part on the right. The split is always at one single point — never scattered."),
                    Block.Para("Every address in the same network has identical network bits. The host bits are what makes each device different."),
                    Block.Bits("mask for /26", Ip.bits(Ip.mask(26)), 26),
                    Block.Para("The prefix is simply how many bits are on the network side. /26 means 26 network bits, which leaves 6 host bits. Those two numbers always add up to 32."),
                    Block.Formula("network bits + host bits = 32"),
                ),
            ),
            Page(
                "Prefix to dotted mask",
                listOf(
                    Block.Para("Write the prefix as that many ones, pad with zeros to 32, cut into four octets, convert each. That is all a mask is."),
                    Block.Work("/26 to a dotted mask", listOf(
                        "26 ones, then 6 zeros:",
                        "11111111 11111111 11111111 11000000",
                        "",
                        "11111111 = 255",
                        "11111111 = 255",
                        "11111111 = 255",
                        "11000000 = 128+64 = 192",
                        "",
                        "/26  =  255.255.255.192",
                    )),
                    Block.Para("In practice you never do that longhand, because an octet of a mask can only ever be one of nine values. Two ones is always 192, four ones is always 240:"),
                    Block.Table(maskOctetTable("ones")),
                    Block.Note("A mask is a run of ones followed by a run of zeros, always. 255.255.240.0 is a mask. 255.0.255.0 is not, and no device will accept it."),
                ),
            ),
            Page(
                "The masks worth knowing by heart",
                listOf(
                    Block.Para("These are the ones that come up constantly. You are not expected to memorise them on day one — they stick after a handful of exercises."),
                    Block.Table(prefixTable(24..30, "addr", "hosts")),
                    Block.Para("And the bigger blocks, used as the network you split up:"),
                    Block.Table(prefixTable(16..23, "addr", "hosts")),
                    Block.Check(
                        question = "What prefix is 255.255.255.224?",
                        answer = "/27",
                        why = "224 is three ones (11100000). 8 + 8 + 8 + 3 = 27.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Inside one block",
        summary = "Block size, the two reserved addresses, and the −2",
        pages = listOf(
            Page(
                "How big is the block?",
                listOf(
                    Block.Para("Count the host bits, then raise two to that power. With h host bits the block holds 2^h addresses."),
                    Block.Formula("block size  =  2^h,   h = 32 − prefix"),
                    Block.Work("a /26", listOf(
                        "h = 32 - 26 = 6",
                        "block = 2^6 = 64 addresses",
                    )),
                    Block.Para("There is a second rule that matters just as much, and it is the one people forget:"),
                    Block.Formula("a block may only start at a multiple of its own size"),
                    Block.Para("A block of 64 starts at 0, 64, 128 or 192 — never at 100. That is not a convention, it is what the mask does: the host bits have to be all zero at the start of a block, and that only happens on those addresses."),
                    Block.Split(
                        caption = "256 addresses, where a /26 is allowed to start",
                        capacity = 256,
                        parts = listOf(
                            SplitPart(".0", 64, "start 0"),
                            SplitPart(".64", 64, "start 64"),
                            SplitPart(".128", 64, "start 128"),
                            SplitPart(".192", 64, "start 192"),
                        ),
                    ),
                ),
            ),
            Page(
                "The four landmarks",
                listOf(
                    Block.Para("Once you know where a block starts and how big it is, everything else is addition. Take 192.168.1.64/26 — a block of 64 starting at .64:"),
                    Block.Work("reading a block off", listOf(
                        "network   192.168.1.64      <- start",
                        "first     192.168.1.65      <- network + 1",
                        "last      192.168.1.126     <- broadcast - 1",
                        "broadcast 192.168.1.127     <- start + 64 - 1",
                    )),
                    Block.Para("The broadcast address is the last address of the block, which is why it is start + size − 1 and not start + size. Counting from 64, the sixty-fourth address is 127, not 128 — 128 already belongs to the next block."),
                    Block.Split(
                        caption = "192.168.1.64/26",
                        capacity = 64,
                        parts = listOf(
                            SplitPart("net .64", 1, "network"),
                            SplitPart("hosts .65 – .126", 62, "62 usable"),
                            SplitPart("bc .127", 1, "broadcast"),
                        ),
                    ),
                ),
            ),
            Page(
                "Why it is minus two",
                listOf(
                    Block.Para("Two addresses in every block are spent on the block itself. The lowest one names the network and the highest one is the broadcast — neither can be given to a device."),
                    Block.Formula("usable hosts  =  2^h − 2"),
                    Block.Para("That is why a /26 holds 64 addresses but only 62 hosts. Turn it around and it becomes the rule you will use constantly when planning:"),
                    Block.Formula("addresses needed  =  hosts + 2"),
                    Block.Table(prefixTable(26..30, "addr", "hosts")),
                    Block.Note("/31 and /32 are the exceptions and both answer 0 usable hosts. RFC 3021 does allow a /31 to carry two routers on a point-to-point link, but unless a question says so, answer 0."),
                    Block.Check(
                        question = "A department has 62 machines. Does a /26 fit?",
                        answer = "Yes, exactly — with nothing to spare.",
                        why = "62 hosts need 64 addresses. A /26 is 64. 63 machines would need a /25.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "The recipe: one address → everything",
        summary = "The six steps, then three worked examples",
        pages = listOf(
            Page(
                "The six steps",
                listOf(
                    Block.Para("This is the whole procedure. It works for any address and any prefix, needs no calculator, and is the one to practise until it is automatic."),
                    Block.Recipe(listOf(
                        RecipeStep("Write the mask. The prefix tells you how many ones; the octet table turns that into a number."),
                        RecipeStep("Find the interesting octet — the one where the mask is neither 255 nor 0. That is where the block boundaries fall."),
                        RecipeStep("Magic number = 256 − that mask octet. Blocks start every magic number in that octet."),
                        RecipeStep("Round the address's value in that octet down to a multiple of the magic number, and zero every octet to the right. That is the network address."),
                        RecipeStep("Broadcast: add the magic number to that octet, subtract one, and set every octet to the right to 255."),
                        RecipeStep("First host = network + 1. Last host = broadcast − 1. Usable = 2^h − 2."),
                    )),
                    Block.Tip("Steps 4 and 5 only ever touch one octet. Everything to the left is copied unchanged, everything to the right is 0 in the network address and 255 in the broadcast."),
                ),
            ),
            Page(
                "Worked example 1 — 192.168.1.200/26",
                listOf(
                    Block.Work("step 1 — the mask", listOf(
                        "/26 = 26 ones",
                        "8 + 8 + 8 + 2  -> last octet has 2 ones",
                        "2 ones = 192",
                        "mask = 255.255.255.192",
                    )),
                    Block.Work("step 2 — interesting octet", listOf(
                        "255 . 255 . 255 . 192",
                        "                   ^ neither 255 nor 0",
                        "-> the 4th octet",
                    )),
                    Block.Work("step 3 — magic number", listOf(
                        "256 - 192 = 64",
                        "blocks start at 0, 64, 128, 192",
                    )),
                    Block.Work("step 4 — network address", listOf(
                        "our 4th octet is 200",
                        "largest multiple of 64 that is <= 200:",
                        "  64 x 3 = 192   (64 x 4 = 256, too big)",
                        "network = 192.168.1.192",
                    )),
                    Block.Work("step 5 — broadcast", listOf(
                        "192 + 64 - 1 = 255",
                        "broadcast = 192.168.1.255",
                    )),
                    Block.Work("step 6 — hosts", listOf(
                        "first = 192.168.1.193",
                        "last  = 192.168.1.254",
                        "h = 32 - 26 = 6",
                        "usable = 2^6 - 2 = 62",
                    )),
                    Block.Bits("192.168.1.200 — the boundary at 26 bits", Ip.bits(Ip.parse("192.168.1.200")!!), 26),
                ),
            ),
            Page(
                "Worked example 2 — 172.16.34.77/20",
                listOf(
                    Block.Para("Same six steps. The only difference is that the boundary now falls in the third octet, so the fourth octet is pure host space."),
                    Block.Work("steps 1 to 3", listOf(
                        "/20 = 8 + 8 + 4",
                        "4 ones = 240",
                        "mask = 255.255.240.0",
                        "interesting octet = 3rd",
                        "magic = 256 - 240 = 16",
                        "blocks: 0, 16, 32, 48, 64, ...",
                    )),
                    Block.Work("step 4 — network", listOf(
                        "3rd octet is 34",
                        "16 x 2 = 32   (16 x 3 = 48, too big)",
                        "-> 32, and everything right of it is 0",
                        "network = 172.16.32.0",
                    )),
                    Block.Work("step 5 — broadcast", listOf(
                        "32 + 16 - 1 = 47",
                        "everything right of it is 255",
                        "broadcast = 172.16.47.255",
                    )),
                    Block.Work("step 6 — hosts", listOf(
                        "first = 172.16.32.1",
                        "last  = 172.16.47.254",
                        "h = 32 - 20 = 12",
                        "usable = 2^12 - 2 = 4094",
                    )),
                    Block.Note("The commonest mistake here is answering 172.16.34.0. The whole point of step 4 is that 34 is not a block start; 32 is."),
                ),
            ),
            Page(
                "Worked example 3 — 10.150.200.30/11",
                listOf(
                    Block.Para("A big one, to show nothing changes. The boundary is in the second octet, so octets three and four are entirely host space."),
                    Block.Work("all six steps", listOf(
                        "/11 = 8 + 3",
                        "3 ones = 224",
                        "mask = 255.224.0.0",
                        "interesting octet = 2nd",
                        "magic = 256 - 224 = 32",
                        "",
                        "2nd octet is 150",
                        "32 x 4 = 128  (32 x 5 = 160, too big)",
                        "network   = 10.128.0.0",
                        "",
                        "128 + 32 - 1 = 159",
                        "broadcast = 10.159.255.255",
                        "",
                        "first = 10.128.0.1",
                        "last  = 10.159.255.254",
                        "h = 32 - 11 = 21",
                        "usable = 2^21 - 2 = 2097150",
                    )),
                    Block.Check(
                        question = "Which network does 10.150.200.30/11 share with 10.130.7.7/11?",
                        answer = "The same one — 10.128.0.0/11.",
                        why = "Both second octets round down to 128, and that is the only octet the mask cuts through.",
                    ),
                ),
            ),
            Page(
                "Checking your answer in ten seconds",
                listOf(
                    Block.Para("Three checks catch nearly every mistake. Run them before you write the answer down."),
                    Block.Bullets(listOf(
                        "The network address must be a multiple of the block size, in the interesting octet.",
                        "Broadcast − network + 1 must equal the block size exactly.",
                        "The given address must sit between the two. If it does not, you rounded the wrong way.",
                    )),
                    Block.Work("checking example 1", listOf(
                        "192 / 64 = 3 exactly           ok",
                        "255 - 192 + 1 = 64             ok",
                        "192 <= 200 <= 255              ok",
                    )),
                    Block.Tip("If the prefix lands exactly on an octet boundary — /8, /16, /24 — there is no partial octet, the mask octet is 0, and the magic number is 256, meaning the whole octet is one block."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "The other method: binary AND",
        summary = "What your teacher may show instead — same answer",
        pages = listOf(
            Page(
                "Two methods, one answer",
                listOf(
                    Block.Para("You will meet two ways of finding a network address, and they are not rivals — the second one is the proof that the first one works."),
                    Block.Bullets(listOf(
                        "The magic number method, in the previous chapter. Decimal arithmetic only. This is what people use when they have to be quick, and it is what network exams are built around.",
                        "The binary AND method, below. Write the address and the mask as bits and combine them. Slower, but it shows exactly why the answer is the answer.",
                    )),
                    Block.Para("Both are pen-and-paper methods. Neither is more correct. If your school teaches the binary one, use it — and use the magic number as a check, because it takes five seconds."),
                ),
            ),
            Page(
                "How the AND works",
                listOf(
                    Block.Para("AND is a bit-by-bit rule: 1 AND 1 is 1, everything else is 0. Because a mask is ones then zeros, ANDing keeps every network bit of the address and wipes every host bit to zero — which is the definition of the network address."),
                    Block.Work("192.168.1.200/26, the long way", listOf(
                        "addr  11000000 10101000 00000001 11001000",
                        "mask  11111111 11111111 11111111 11000000",
                        "AND   -------- -------- -------- --------",
                        "      11000000 10101000 00000001 11000000",
                        "",
                        "        192  .  168  .    1   .   192",
                    )),
                    Block.Para("For the broadcast address, keep the network bits and set every host bit to 1 instead:"),
                    Block.Work("broadcast, the long way", listOf(
                        "net   11000000 10101000 00000001 11000000",
                        "host bits all 1:",
                        "      11000000 10101000 00000001 11111111",
                        "",
                        "        192  .  168  .    1   .   255",
                    )),
                    Block.Para("The same 192.168.1.192 and 192.168.1.255 the magic number gave, in about eight times the writing."),
                ),
            ),
            Page(
                "Which to use, and when",
                listOf(
                    Block.Table(listOf(
                        "Under time pressure" to "magic number",
                        "Homework showing your work" to "binary AND",
                        "Checking a result" to "the other one",
                        "Understanding why" to "binary AND",
                    )),
                    Block.Para("A useful compromise: convert only the interesting octet to binary. The other three are either copied unchanged or zeroed, so writing them out in bits proves nothing."),
                    Block.Work("only the octet that matters", listOf(
                        "200  = 11001000",
                        "192m = 11000000   (mask octet)",
                        "AND  = 11000000 = 192",
                    )),
                    Block.Tip("This app's step-by-step solutions use the magic number method and show the bit strip alongside, so you can see both at once."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "VLSM: splitting one network up",
        summary = "Different sized subnets from one block",
        pages = listOf(
            Page(
                "What the assignment looks like",
                listOf(
                    Block.Para("A VLSM task gives you one network and a list of departments with how many devices each one has:"),
                    Block.Work("a typical assignment", listOf(
                        "Split 10.0.0.0/24 into:",
                        "  A - 60 hosts",
                        "  B - 30 hosts",
                        "  C - 14 hosts",
                        "  D -  7 hosts",
                    )),
                    Block.Para("You must give each one a block big enough, and waste as little as possible. Giving all four a /26 would work arithmetically and would be wrong — it wastes most of the network and no marker accepts it."),
                    Block.Para("The answer is a table: for each subnet its network address, prefix, host range and broadcast."),
                ),
            ),
            Page(
                "The five steps",
                listOf(
                    Block.Recipe(listOf(
                        RecipeStep("Sort the subnets by host count, largest first. Do this before anything else."),
                        RecipeStep("Take the largest. Add 2 to its host count, then round up to the next power of two. That is its block size."),
                        RecipeStep("Host bits h is the exponent you just used; the prefix is 32 − h."),
                        RecipeStep("Place the block at the first free address. Read off its network, first host, last host and broadcast exactly as in the previous chapter."),
                        RecipeStep("Move to the address just after that broadcast, and repeat with the next subnet down."),
                    )),
                    Block.Formula("hosts + 2  →  round up to 2^h  →  prefix = 32 − h"),
                    Block.Note("Round up, never to the nearest. 7 hosts need 9 addresses, and 8 is not enough — it takes a block of 16."),
                ),
            ),
            Page(
                "Why largest first, really",
                listOf(
                    Block.Para("This is the step people skip, and it is the one that makes the method work. A block may only start at a multiple of its own size, so a big block needs a start address that a small block can easily block off."),
                    Block.Para("Do it in the wrong order — D's 16 addresses first, then A's 64:"),
                    Block.Work("smallest first, going wrong", listOf(
                        "D (/28, 16) at 10.0.0.0  -> ends .15",
                        "cursor now at 10.0.0.16",
                        "",
                        "A needs a block of 64.",
                        "may start at 0, 64, 128, 192 only.",
                        "16 is not a multiple of 64. Nor 32, nor 48.",
                        "-> A must jump to 10.0.0.64",
                        "",
                        ".16 to .63 = 48 addresses stranded",
                    )),
                    Block.Split(
                        caption = "Smallest first — the gap nothing can use",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("D · 16", 16, "placed first"),
                            SplitPart("stranded · 48", 48, "wasted", free = true),
                            SplitPart("A · 64", 64, "forced up here"),
                            SplitPart("rest", 128, "", free = true),
                        ),
                    ),
                    Block.Para("Going largest first, that can never happen: after placing a block of 64 the cursor is at a multiple of 64, which is automatically also a multiple of 32, 16, 8 and 4. Every smaller block that follows lands on a legal start address without you having to think about it."),
                    Block.Tip("That is the entire reason the first stage of the exercise makes you drag the subnets into order before it lets you calculate anything."),
                ),
            ),
            Page(
                "Worked example — 10.0.0.0/24",
                listOf(
                    Block.Work("step 1 — sort", listOf(
                        "A 60, B 30, C 14, D 7   (already in order)",
                        "total demand check:",
                        "64 + 32 + 16 + 16 = 128 <= 256   fits",
                    )),
                    Block.Work("A - 60 hosts", listOf(
                        "60 + 2 = 62",
                        "2^5 = 32 too small, 2^6 = 64 ok",
                        "h = 6, prefix = 32 - 6 = /26",
                        "starts at 10.0.0.0",
                        "network   10.0.0.0/26",
                        "first     10.0.0.1",
                        "last      10.0.0.62",
                        "broadcast 10.0.0.63",
                        "next free 10.0.0.64",
                    )),
                    Block.Work("B - 30 hosts", listOf(
                        "30 + 2 = 32",
                        "2^5 = 32 exactly ok",
                        "h = 5, prefix = /27",
                        "64 is a multiple of 32, so B starts there",
                        "network   10.0.0.64/27",
                        "first     10.0.0.65",
                        "last      10.0.0.94",
                        "broadcast 10.0.0.95",
                        "next free 10.0.0.96",
                    )),
                    Block.Work("C - 14 hosts", listOf(
                        "14 + 2 = 16 -> 2^4, h = 4, /28",
                        "network   10.0.0.96/28",
                        "first     10.0.0.97",
                        "last      10.0.0.110",
                        "broadcast 10.0.0.111",
                        "next free 10.0.0.112",
                    )),
                    Block.Work("D - 7 hosts", listOf(
                        "7 + 2 = 9",
                        "2^3 = 8 too small, 2^4 = 16 ok",
                        "h = 4, /28  (same as C)",
                        "network   10.0.0.112/28",
                        "first     10.0.0.113",
                        "last      10.0.0.126",
                        "broadcast 10.0.0.127",
                        "next free 10.0.0.128",
                    )),
                    Block.Split(
                        caption = "The finished plan",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("A /26", 64, "60 hosts"),
                            SplitPart("B /27", 32, "30 hosts"),
                            SplitPart("C /28", 16, "14 hosts"),
                            SplitPart("D /28", 16, "7 hosts"),
                            SplitPart("free", 128, "10.0.0.128 – .255", free = true),
                        ),
                    ),
                    Block.Note("D is the interesting one: 7 hosts need 9 addresses, 8 is one short, so D takes a /28 exactly like C. Rounding up costs addresses and there is no way around it."),
                ),
            ),
            Page(
                "Worked example — crossing octets",
                listOf(
                    Block.Para("Same method on a bigger network, where blocks are larger than one octet. Split 192.168.8.0/22 — 1024 addresses, 192.168.8.0 to 192.168.11.255."),
                    Block.Work("sorted, then placed", listOf(
                        "Sales 500 | Ops 200 | Lab 100 | Wifi 60 | Link 2",
                        "",
                        "Sales 500+2=502 -> 512 = 2^9  -> /23",
                        "  192.168.8.0/23",
                        "  hosts 192.168.8.1 - 192.168.9.254",
                        "  bc    192.168.9.255",
                        "",
                        "Ops   200+2=202 -> 256 = 2^8  -> /24",
                        "  192.168.10.0/24",
                        "  hosts .1 - .254   bc 192.168.10.255",
                        "",
                        "Lab   100+2=102 -> 128 = 2^7  -> /25",
                        "  192.168.11.0/25",
                        "  hosts .1 - .126   bc 192.168.11.127",
                        "",
                        "Wifi   60+2= 62 ->  64 = 2^6  -> /26",
                        "  192.168.11.128/26",
                        "  hosts .129 - .190  bc 192.168.11.191",
                        "",
                        "Link    2+2=  4 ->   4 = 2^2  -> /30",
                        "  192.168.11.192/30",
                        "  hosts .193 - .194  bc 192.168.11.195",
                        "",
                        "used 512+256+128+64+4 = 964 of 1024",
                        "free 192.168.11.196 - .255  (60 left)",
                    )),
                    Block.Para("A block of 512 is two full 256-address ranges, so Sales' host range runs across the boundary from .8 into .9. Nothing about the method changes — the arithmetic is still done in the interesting octet, which for a /23 is the third: the mask is 255.255.254.0, the magic number is 2, and blocks start at every even value of the third octet."),
                    Block.Tip("A /30 with 2 usable hosts is the standard size for a link between two routers. It shows up in almost every assignment."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Traps and the final check",
        summary = "The mistakes that cost marks",
        pages = listOf(
            Page(
                "Seven ways to get it wrong",
                listOf(
                    Block.Bullets(listOf(
                        "Forgetting the +2. 62 hosts fit in a /26, but 62 addresses do not — you need 64.",
                        "Rounding to the nearest power of two instead of up. 9 addresses need 16, not 8.",
                        "Sorting smallest first, so the big block has nowhere legal to land.",
                        "Starting a block at an address that is not a multiple of its size — a /28 may begin at .96 or .112, never at .100.",
                        "Counting the 255s instead of the ones. 255.255.255.192 is /26, not /24.",
                        "Assuming a broadcast address ends in 255. In a /26 it is .63, .127, .191 or .255.",
                        "Answering 2 usable hosts for a /31. Right in a router config, wrong on nearly every exam.",
                    )),
                ),
            ),
            Page(
                "Checking a finished plan",
                listOf(
                    Block.Para("Two checks catch a broken VLSM plan immediately:"),
                    Block.Bullets(listOf(
                        "Every network address is a multiple of its own block size.",
                        "Each subnet's broadcast is exactly one below the next subnet's network address — no gaps, no overlaps.",
                    )),
                    Block.Work("checking the 10.0.0.0/24 plan", listOf(
                        "  0 / 64 = 0 exact     bc  63 -> next  64  ok",
                        " 64 / 32 = 2 exact     bc  95 -> next  96  ok",
                        " 96 / 16 = 6 exact     bc 111 -> next 112  ok",
                        "112 / 16 = 7 exact     bc 127 -> free 128  ok",
                    )),
                    Block.Check(
                        question = "A plan puts a /27 at 10.0.0.80. Is that legal?",
                        answer = "No.",
                        why = "A /27 is a block of 32 and must start at a multiple of 32: 64 or 96. 80 is a multiple of 16 only.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "The cheat sheet",
        summary = "Everything on one page",
        pages = listOf(
            Page(
                "What to write in the margin",
                listOf(
                    Block.Heading("Powers of two"),
                    Block.Formula("2 4 8 16 32 64 128 256 512 1024"),
                    Block.Heading("Mask octet values"),
                    Block.Formula("0 128 192 224 240 248 252 254 255"),
                    Block.Heading("One address → everything"),
                    Block.Recipe(listOf(
                        RecipeStep("mask from the prefix"),
                        RecipeStep("interesting octet = the one that is not 255 or 0"),
                        RecipeStep("magic = 256 − that octet"),
                        RecipeStep("round the address down to a multiple of magic → network"),
                        RecipeStep("network + magic − 1 in that octet, 255 to the right → broadcast"),
                        RecipeStep("first = net + 1, last = bc − 1, usable = 2^h − 2"),
                    )),
                    Block.Heading("Splitting a network"),
                    Block.Recipe(listOf(
                        RecipeStep("sort largest first"),
                        RecipeStep("hosts + 2, round up to 2^h"),
                        RecipeStep("prefix = 32 − h"),
                        RecipeStep("place at the next free address, read the block off"),
                        RecipeStep("cursor = broadcast + 1, repeat"),
                    )),
                    Block.Table(prefixTable(24..30, "addr", "hosts")),
                ),
            ),
        ),
    ),
)
