package com.n3d.netlab.i18n

import com.n3d.netlab.core.Ip

/**
 * Tables both courses share.
 *
 * Generated from `Ip` rather than typed out, so the table a learner memorises
 * from is by construction the same arithmetic the grader marks them against.
 */
internal fun prefixTable(range: IntProgression, addrWord: String, hostWord: String) =
    range.map { p ->
        "/$p" to "${Ip.format(Ip.mask(p))}  ·  ${Ip.blockSize(p)} $addrWord  ·  ${Ip.usableHosts(p)} $hostWord"
    }

/** 2^1 … 2^16, the ladder every subnetting question is climbed with. */
internal fun powersTable(range: IntRange = 1..16) =
    range.map { n -> "2^$n" to (1L shl n).toString() }

/** The only nine values an octet of a valid mask can ever have. */
internal fun maskOctetTable(onesWord: String) =
    (0..8).map { ones ->
        val value = if (ones == 0) 0 else (255 shl (8 - ones)) and 0xFF
        "$value" to "${"1".repeat(ones)}${"0".repeat(8 - ones)}  ·  $ones $onesWord"
    }
