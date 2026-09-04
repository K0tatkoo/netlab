package com.n3d.netlab.i18n

import com.n3d.netlab.core.Ip

/**
 * The reference table both languages share.
 *
 * Generated from `Ip` rather than typed out, so the table a learner memorises
 * from is by construction the same arithmetic the grader marks them against.
 */
internal fun prefixTable(range: IntProgression, addrWord: String, hostWord: String) =
    range.map { p ->
        "/$p" to "${Ip.format(Ip.mask(p))}  ·  ${Ip.blockSize(p)} $addrWord  ·  ${Ip.usableHosts(p)} $hostWord"
    }

internal val enLessons: List<Lesson> = listOf(

    Lesson(
        title = "An address is one 32-bit number",
        summary = "Why the dots are only punctuation",
        body = listOf(
            Block.Para("An IPv4 address is a single 32-bit number. The four numbers separated by dots are nothing more than a readable way of writing it: each one is eight of those bits, which is why none of them can go above 255."),
            Block.Bits("192.168.1.10 in binary", Ip.bits(Ip.parse("192.168.1.10")!!), 24),
            Block.Para("Inside one octet each bit is worth double the one to its right:"),
            Block.Formula("128  64  32  16  8  4  2  1"),
            Block.Para("So 192 is 128 + 64, which is 11000000. Adding every bit gives 255, and that is the largest an octet can be. Being fluent at converting one octet in your head is most of what makes subnetting feel quick."),
            Block.Note("Everything else in this app is arithmetic on that one 32-bit number. The dots never take part in the calculation."),
        ),
    ),

    Lesson(
        title = "The mask decides where the network ends",
        summary = "Prefix, dotted mask, and what they really say",
        body = listOf(
            Block.Para("An address on its own does not say which network it belongs to. The mask does. It splits the 32 bits into a network part on the left and a host part on the right — and the split is always at one point, never scattered."),
            Block.Para("The prefix is just how many bits are on the network side. A /24 means 24 network bits and 8 host bits. Write 24 ones then 8 zeros and you have the dotted mask:"),
            Block.Bits("mask for /24", Ip.bits(Ip.mask(24)), 24),
            Block.Formula("11111111.11111111.11111111.00000000  =  255.255.255.0"),
            Block.Para("Everything else falls out of the host bits. With h host bits the block holds 2^h addresses, and it may only start at an address that is a multiple of 2^h."),
            Block.Para("The masks worth knowing by heart:"),
            Block.Table(prefixTable(24..30, "addr", "hosts")),
            Block.Para("And the larger ones, for base networks:"),
            Block.Table(prefixTable(16..23, "addr", "hosts")),
            Block.Note("A mask is always a run of ones followed by a run of zeros. 255.255.240.0 is a mask; 255.0.255.0 is not, and no device will accept it."),
        ),
    ),

    Lesson(
        title = "Network, broadcast, and the −2",
        summary = "The two addresses you never get to use",
        body = listOf(
            Block.Para("Every block spends two of its addresses on itself. The lowest one — all host bits zero — names the network. The highest one — all host bits one — is the broadcast address for everything inside it. Neither can be configured on a machine."),
            Block.Formula("usable hosts  =  2^h − 2"),
            Block.Para("That is why a /26 holds 64 addresses but only 62 hosts, and why a department of 62 machines fits in a /26 while a department of 63 does not and needs a /25."),
            Block.Para("Reading a block off, in order:"),
            Block.Bullets(listOf(
                "Network address — the block's own address, host bits all 0",
                "First host — network + 1",
                "Last host — broadcast − 1",
                "Broadcast — host bits all 1, also network + block size − 1",
            )),
            Block.Note("/31 and /32 are the exceptions. A /31 has no room for both, and a /32 is a single address; both answer 0 usable hosts. RFC 3021 does let a /31 carry two routers on a point-to-point link, but unless a question says so, answer 0."),
        ),
    ),

    Lesson(
        title = "The magic number",
        summary = "Finding any network address in seconds, no binary",
        body = listOf(
            Block.Para("You almost never need to write out binary. Find the one octet where the mask is neither 255 nor 0 — call it the interesting octet — and subtract it from 256. That is the magic number, and it is the block size measured in that octet."),
            Block.Formula("magic  =  256 − mask octet"),
            Block.Para("Worked example: which network does 172.16.34.77/20 belong to?"),
            Block.Bullets(listOf(
                "/20 → mask 255.255.240.0, so the third octet is the interesting one.",
                "256 − 240 = 16. Blocks start every 16 in the third octet: 0, 16, 32, 48, 64…",
                "The address has 34 in that octet. Round down to the nearest multiple of 16 → 32.",
                "Zero everything after it: the network is 172.16.32.0.",
                "The broadcast is one block up, minus one: 32 + 16 − 1 = 47, and the rest goes to 255 → 172.16.47.255.",
                "Hosts run 172.16.32.1 to 172.16.47.254 — that is 4094 of them.",
            )),
            Block.Para("The whole trick is rounding down to a multiple of the magic number. Everything to the right of the interesting octet is host space and simply runs 0 to 255."),
            Block.Note("If the prefix lands exactly on an octet boundary — /8, /16, /24 — there is no partial octet. Blocks then step by 1 in the octet after the last full 255."),
        ),
    ),

    Lesson(
        title = "VLSM — subnets of different sizes",
        summary = "The method, and the example it is always taught with",
        body = listOf(
            Block.Para("Splitting a network into equal pieces is easy but wasteful. If one department has 60 machines and another has 7, giving both a /26 throws away 55 addresses. Variable-length subnet masking gives each subnet only the block it actually needs."),
            Block.Para("The method is four moves, and the order matters:"),
            Block.Bullets(listOf(
                "Sort the subnets by size, largest first.",
                "For each one: hosts + 2, round up to the next power of two, and that exponent is the host bits. Prefix = 32 − host bits.",
                "Place it at the first free address. Working downwards, that address is always already a multiple of the block size.",
                "Move the cursor on by the block size and repeat.",
            )),
            Block.Para("Take 10.0.0.0/24 split into A: 60 hosts, B: 30, C: 14, D: 7."),
            Block.Para("A needs 60 + 2 = 62 addresses. 2^5 = 32 is too small, 2^6 = 64 works, so 6 host bits and a /26 — a block of 64 starting at 10.0.0.0."),
            Block.Para("B needs 32. 2^5 = 32 exactly, so a /27. The cursor is at 10.0.0.64, which is a multiple of 32, so B lands there."),
            Block.Para("C needs 16 → /28 at 10.0.0.96. D needs 9, which still rounds up to 16 → /28 at 10.0.0.112."),
            Block.Table(listOf(
                "A  ·  60 hosts" to "10.0.0.0/26 · .1 – .62 · bc .63",
                "B  ·  30 hosts" to "10.0.0.64/27 · .65 – .94 · bc .95",
                "C  ·  14 hosts" to "10.0.0.96/28 · .97 – .110 · bc .111",
                "D  ·  7 hosts" to "10.0.0.112/28 · .113 – .126 · bc .127",
            )),
            Block.Para("128 of the 256 addresses are used, and 10.0.0.128 – 10.0.0.255 is still free — room for another /25, or for all four departments to double."),
            Block.Note("D is the interesting one. Seven hosts need 9 addresses, and 2^3 = 8 is one short, so D gets a /28 exactly like C. Rounding up is what costs the addresses, and it is unavoidable."),
        ),
    ),

    Lesson(
        title = "Where it usually goes wrong",
        summary = "Six mistakes worth recognising",
        body = listOf(
            Block.Bullets(listOf(
                "Forgetting the +2. 62 hosts fit in a /26, but 62 addresses do not — you need 64.",
                "Sorting smallest first. The big block then has nowhere aligned to land and the space between is wasted.",
                "Starting a block at an address that is not a multiple of its size. A /28 may begin at .96 or .112, never at .100.",
                "Reading the mask instead of the prefix. 255.255.255.192 is /26, not /24 — count the ones, do not count the 255s.",
                "Assuming the broadcast address always ends in 255. In a /26 it is .63, .127, .191 or .255.",
                "Answering 2 usable hosts for a /31. Correct in a router's config, wrong on nearly every exam.",
            )),
            Block.Note("If a plan looks right, check it two ways: every network address should be a multiple of its own block size, and each subnet's broadcast address should be exactly one below the next subnet's network address."),
        ),
    ),
)
