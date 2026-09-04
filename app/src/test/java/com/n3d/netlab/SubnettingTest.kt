package com.n3d.netlab

import com.n3d.netlab.core.Difficulty
import com.n3d.netlab.core.Generator
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.Requirement
import com.n3d.netlab.core.Vlsm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IpTest {

    @Test
    fun `parses and formats dotted quads`() {
        assertEquals(0L, Ip.parse("0.0.0.0"))
        assertEquals(Ip.MAX, Ip.parse("255.255.255.255"))
        assertEquals(3232235786L, Ip.parse("192.168.1.10"))
        assertEquals("192.168.1.10", Ip.format(3232235786L))
    }

    @Test
    fun `rejects malformed addresses`() {
        assertNull(Ip.parse("192.168.1"))
        assertNull(Ip.parse("192.168.1.256"))
        assertNull(Ip.parse("192.168.1.1.1"))
        assertNull(Ip.parse("192.168.1.a"))
        // Leading zeros are ambiguous between octal and decimal readings.
        assertNull(Ip.parse("192.168.01.1"))
    }

    @Test
    fun `masks round-trip through prefixes`() {
        assertEquals(0L, Ip.mask(0))
        assertEquals(Ip.parse("255.255.255.192"), Ip.mask(26))
        assertEquals(Ip.MAX, Ip.mask(32))
        for (p in 0..32) assertEquals(p, Ip.prefixOfMask(Ip.mask(p)))
        // A mask must be a contiguous run of ones.
        assertNull(Ip.prefixOfMask(Ip.parse("255.0.255.0")!!))
    }

    @Test
    fun `host counts account for network and broadcast`() {
        assertEquals(254L, Ip.usableHosts(24))
        assertEquals(62L, Ip.usableHosts(26))
        assertEquals(2L, Ip.usableHosts(30))
        assertEquals(0L, Ip.usableHosts(31))
        assertEquals(0L, Ip.usableHosts(32))
    }

    @Test
    fun `prefix for a host count rounds up past the plus two`() {
        assertEquals(26, Ip.prefixFor(62))
        // 63 hosts need 65 addresses, which no /26 can hold.
        assertEquals(25, Ip.prefixFor(63))
        assertEquals(27, Ip.prefixFor(30))
        assertEquals(28, Ip.prefixFor(14))
        // Seven hosts need nine addresses, so a /29's eight are one short.
        assertEquals(28, Ip.prefixFor(7))
        assertEquals(29, Ip.prefixFor(6))
        assertEquals(30, Ip.prefixFor(2))
    }

    @Test
    fun `magic number names the octet a block steps through`() {
        assertEquals(4 to 64L, Ip.magicNumber(26))
        assertEquals(4 to 1L, Ip.magicNumber(32))
        assertEquals(3 to 1L, Ip.magicNumber(24))
        assertEquals(3 to 16L, Ip.magicNumber(20))
        assertEquals(3 to 4L, Ip.magicNumber(22))
        assertEquals(1 to 1L, Ip.magicNumber(8))
    }

    @Test
    fun `analysis of a mid-block address`() {
        val addr = Ip.parse("172.16.34.77")!!
        assertEquals(Ip.parse("172.16.32.0"), Ip.networkOf(addr, 20))
        assertEquals(Ip.parse("172.16.47.255"), Ip.broadcastOf(addr, 20))
        assertEquals(Ip.parse("172.16.32.1"), Ip.firstHostOf(addr, 20))
        assertEquals(Ip.parse("172.16.47.254"), Ip.lastHostOf(addr, 20))
        assertEquals(4094L, Ip.usableHosts(20))
    }
}

class VlsmTest {

    /** The assignment the app was asked for, worked by hand and pinned here. */
    @Test
    fun `splits 10_0_0_0 slash 24 into 60, 30, 14 and 7 hosts`() {
        val plan = Vlsm.plan(
            Ip.parse("10.0.0.0")!!,
            24,
            listOf(
                Requirement("A", 60),
                Requirement("B", 30),
                Requirement("C", 14),
                Requirement("D", 7),
            ),
        )

        assertTrue(plan.fits)
        val a = plan.forName("A")!!
        assertEquals(26, a.prefix)
        assertEquals(Ip.parse("10.0.0.0"), a.network)
        assertEquals(Ip.parse("10.0.0.1"), a.firstHost)
        assertEquals(Ip.parse("10.0.0.62"), a.lastHost)
        assertEquals(Ip.parse("10.0.0.63"), a.broadcast)

        val b = plan.forName("B")!!
        assertEquals(27, b.prefix)
        assertEquals(Ip.parse("10.0.0.64"), b.network)
        assertEquals(Ip.parse("10.0.0.95"), b.broadcast)

        val c = plan.forName("C")!!
        assertEquals(28, c.prefix)
        assertEquals(Ip.parse("10.0.0.96"), c.network)
        assertEquals(Ip.parse("10.0.0.111"), c.broadcast)

        // Seven hosts round up to the same /28 as fourteen — the point of the
        // whole exercise, and the answer people most often get wrong.
        val d = plan.forName("D")!!
        assertEquals(28, d.prefix)
        assertEquals(Ip.parse("10.0.0.112"), d.network)
        assertEquals(Ip.parse("10.0.0.127"), d.broadcast)

        assertEquals(128L, plan.used)
        assertEquals(128L, plan.free)
        assertEquals(Ip.parse("10.0.0.128"), plan.nextFree)
    }

    @Test
    fun `allocates largest first regardless of the order given`() {
        val shuffled = Vlsm.plan(
            Ip.parse("192.168.1.0")!!,
            24,
            listOf(Requirement("D", 7), Requirement("A", 60), Requirement("C", 14), Requirement("B", 30)),
        )
        assertEquals(Ip.parse("192.168.1.0"), shuffled.forName("A")!!.network)
        assertEquals(Ip.parse("192.168.1.64"), shuffled.forName("B")!!.network)
        assertEquals(Ip.parse("192.168.1.96"), shuffled.forName("C")!!.network)
        assertEquals(Ip.parse("192.168.1.112"), shuffled.forName("D")!!.network)
    }

    @Test
    fun `every block starts on its own boundary and none overlap`() {
        val plan = Vlsm.plan(
            Ip.parse("172.16.0.0")!!,
            22,
            listOf(
                Requirement("A", 500),
                Requirement("B", 200),
                Requirement("C", 100),
                Requirement("D", 50),
                Requirement("E", 2),
            ),
        )
        assertTrue(plan.fits)
        val ordered = plan.allocations.sortedBy { it.network }
        ordered.forEach { alloc ->
            assertEquals(0L, alloc.network % alloc.blockSize)
            assertTrue(alloc.usable >= alloc.requirement.hosts)
        }
        ordered.zipWithNext().forEach { (first, second) ->
            assertEquals(first.broadcast + 1, second.network)
        }
    }

    @Test
    fun `reports what cannot be placed`() {
        val plan = Vlsm.plan(
            Ip.parse("192.168.1.0")!!,
            24,
            listOf(Requirement("A", 200), Requirement("B", 100)),
        )
        assertEquals(1, plan.allocations.size)
        assertEquals(1, plan.unplaced.size)
        assertEquals("B", plan.unplaced.first().name)
    }

    /**
     * The generator promises two things the practice screen depends on: the
     * assignment always fits, and each host count forces exactly one block
     * size, so there is only one right answer to mark against.
     */
    @Test
    fun `generated exercises are solvable and unambiguous`() {
        val random = Random(20260904)
        Difficulty.entries.forEach { difficulty ->
            repeat(300) {
                val task = Generator.vlsm(difficulty, random)
                val plan = task.plan
                assertTrue("$difficulty task did not fit: ${task.label}", plan.fits)
                assertTrue(plan.used <= plan.capacity)
                plan.allocations.forEach { alloc ->
                    assertEquals(0L, alloc.network % alloc.blockSize)
                    assertTrue(alloc.usable >= alloc.requirement.hosts)
                    // One size smaller would genuinely not be enough.
                    assertTrue(Ip.usableHosts(alloc.prefix + 1) < alloc.requirement.hosts)
                }
            }
        }
    }

    @Test
    fun `generated analysis tasks sit inside their own block`() {
        val random = Random(7)
        Difficulty.entries.forEach { difficulty ->
            repeat(300) {
                val task = Generator.analyze(difficulty, random)
                assertEquals(task.network, Ip.networkOf(task.address, task.prefix))
                assertTrue(task.address in task.network..task.broadcast)
            }
        }
    }
}
