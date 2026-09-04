package com.n3d.netlab.core

/**
 * IPv4 address arithmetic.
 *
 * Addresses are carried as a `Long` in 0..0xFFFFFFFF rather than the more
 * obvious `Int`. An IPv4 address is unsigned, and the moment one lands in a
 * signed 32-bit Int every comparison above 127.255.255.255 flips sign — so
 * `192.168.1.0 < 10.0.0.0` comes out true and a VLSM allocator walking upwards
 * through the space silently stops. Long has room for the whole range plus the
 * 2^32 "one past the end" cursor the allocator needs, so ordinary `<` and `+`
 * work everywhere and there is no unsigned-comparison helper to forget.
 */
object Ip {

    const val MAX: Long = 0xFFFF_FFFFL

    /** Number of addresses in the whole space — also the exclusive end cursor. */
    const val SPACE: Long = 0x1_0000_0000L

    // ---- parsing / formatting ------------------------------------------------

    /**
     * Strict dotted-quad parse. Returns null on anything malformed.
     *
     * Leading zeros are rejected on purpose: `010` is 8 to `inet_aton` and 10 to
     * most calculators, and a subnetting trainer that quietly picks one of the
     * two would be teaching the wrong lesson.
     */
    fun parse(text: String): Long? {
        val parts = text.trim().split('.')
        if (parts.size != 4) return null
        var acc = 0L
        for (p in parts) {
            if (p.isEmpty() || p.length > 3) return null
            if (!p.all { it in '0'..'9' }) return null
            if (p.length > 1 && p[0] == '0') return null
            val v = p.toInt()
            if (v > 255) return null
            acc = (acc shl 8) or v.toLong()
        }
        return acc
    }

    /** Parses `10.0.0.0/24`. The prefix part is required. */
    fun parseCidr(text: String): Pair<Long, Int>? {
        val t = text.trim()
        val slash = t.indexOf('/')
        if (slash <= 0) return null
        val addr = parse(t.substring(0, slash)) ?: return null
        val prefix = t.substring(slash + 1).trim().toIntOrNull() ?: return null
        if (prefix !in 0..32) return null
        return addr to prefix
    }

    /** Accepts `/26`, `26`, or a dotted mask `255.255.255.192`. */
    fun parsePrefix(text: String): Int? {
        val t = text.trim().removePrefix("/")
        if (t.isEmpty()) return null
        if ('.' in t) {
            val m = parse(t) ?: return null
            return prefixOfMask(m)
        }
        return t.toIntOrNull()?.takeIf { it in 0..32 }
    }

    fun format(addr: Long): String {
        val a = addr and MAX
        return "${(a ushr 24) and 0xFF}.${(a ushr 16) and 0xFF}.${(a ushr 8) and 0xFF}.${a and 0xFF}"
    }

    fun cidr(addr: Long, prefix: Int): String = "${format(addr)}/$prefix"

    fun octet(addr: Long, index: Int): Int = ((addr ushr (24 - index * 8)) and 0xFF).toInt()

    // ---- masks ---------------------------------------------------------------

    fun mask(prefix: Int): Long = if (prefix <= 0) 0L else (MAX shl (32 - prefix)) and MAX

    fun wildcard(prefix: Int): Long = MAX xor mask(prefix)

    /** Null if the value is not a contiguous run of 1s (e.g. 255.0.255.0). */
    fun prefixOfMask(m: Long): Int? {
        val v = m and MAX
        for (p in 0..32) if (mask(p) == v) return p
        return null
    }

    // ---- block geometry ------------------------------------------------------

    /** Total addresses in a block of this prefix, network and broadcast included. */
    fun blockSize(prefix: Int): Long = 1L shl (32 - prefix)

    /**
     * Usable host addresses.
     *
     * /31 and /32 are deliberately 0 here. RFC 3021 does allow both addresses of
     * a /31 to be used on a point-to-point link, but every exam and every
     * textbook the app is teaching against answers 0, and the Learn section
     * calls the exception out rather than the arithmetic quietly assuming it.
     */
    fun usableHosts(prefix: Int): Long = if (prefix >= 31) 0L else blockSize(prefix) - 2

    fun networkOf(addr: Long, prefix: Int): Long = addr and mask(prefix)

    fun broadcastOf(addr: Long, prefix: Int): Long = networkOf(addr, prefix) or wildcard(prefix)

    fun firstHostOf(addr: Long, prefix: Int): Long? =
        if (prefix >= 31) null else networkOf(addr, prefix) + 1

    fun lastHostOf(addr: Long, prefix: Int): Long? =
        if (prefix >= 31) null else broadcastOf(addr, prefix) - 1

    fun isNetworkAddress(addr: Long, prefix: Int): Boolean = networkOf(addr, prefix) == addr

    /**
     * The smallest prefix whose block holds `hosts` usable addresses.
     *
     * The `+2` for the network and broadcast address is the single most common
     * slip in the whole topic — 62 hosts needing a /26 and not a /27 — so it
     * lives in one function that everything else calls.
     */
    fun hostBitsFor(hosts: Int): Int {
        var bits = 2
        while (bits < 32 && (1L shl bits) - 2 < hosts) bits++
        return bits
    }

    fun prefixFor(hosts: Int): Int = 32 - hostBitsFor(hosts)

    /**
     * Which octet a block boundary steps through, and by how much — the
     * "magic number". A /26 gives (4, 64): boundaries every 64 in the fourth
     * octet. A /22 gives (3, 4): every 4 in the third.
     */
    fun magicNumber(prefix: Int): Pair<Int, Long> {
        val hostBits = 32 - prefix
        // /0 has no boundary inside the space at all; report the first octet so
        // callers have something sane to print.
        if (hostBits >= 32) return 1 to 256L
        val octet = 4 - (hostBits / 8)
        val increment = 1L shl (hostBits % 8)
        return octet to increment
    }

    // ---- binary --------------------------------------------------------------

    fun toBinary(addr: Long, separator: String = "."): String =
        (0..3).joinToString(separator) { i ->
            octet(addr, i).toString(2).padStart(8, '0')
        }

    /** Bits only, no separators — for the boundary strip in the UI. */
    fun bits(addr: Long): String = toBinary(addr, "")

    // ---- classification ------------------------------------------------------

    enum class Klass { A, B, C, D, E, Loopback, ThisNetwork }

    fun klass(addr: Long): Klass {
        val first = octet(addr, 0)
        return when {
            first == 0 -> Klass.ThisNetwork
            first == 127 -> Klass.Loopback
            first < 128 -> Klass.A
            first < 192 -> Klass.B
            first < 224 -> Klass.C
            first < 240 -> Klass.D
            else -> Klass.E
        }
    }

    /** The default (classful) prefix, back when the first octet decided it. */
    fun classfulPrefix(addr: Long): Int? = when (klass(addr)) {
        Klass.A -> 8
        Klass.B -> 16
        Klass.C -> 24
        else -> null
    }

    enum class Scope { Private, Loopback, LinkLocal, Cgnat, Multicast, Reserved, Public }

    fun scope(addr: Long): Scope = when {
        inBlock(addr, parse("10.0.0.0")!!, 8) -> Scope.Private
        inBlock(addr, parse("172.16.0.0")!!, 12) -> Scope.Private
        inBlock(addr, parse("192.168.0.0")!!, 16) -> Scope.Private
        inBlock(addr, parse("127.0.0.0")!!, 8) -> Scope.Loopback
        inBlock(addr, parse("169.254.0.0")!!, 16) -> Scope.LinkLocal
        inBlock(addr, parse("100.64.0.0")!!, 10) -> Scope.Cgnat
        klass(addr) == Klass.D -> Scope.Multicast
        klass(addr) == Klass.E -> Scope.Reserved
        else -> Scope.Public
    }

    fun inBlock(addr: Long, network: Long, prefix: Int): Boolean =
        networkOf(addr, prefix) == networkOf(network, prefix)
}
