package ch.rhosys.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLoggerTest {

    private val logger = AppLogger()

    @Test
    fun `info appends entry with INFO level`() {
        logger.info("hello")
        val entries = logger.entries.value
        assertEquals(1, entries.size)
        assertEquals(LogLevel.INFO, entries[0].level)
        assertEquals("hello", entries[0].message)
    }

    @Test
    fun `warn appends entry with WARN level`() {
        logger.warn("caution")
        val entries = logger.entries.value
        assertEquals(1, entries.size)
        assertEquals(LogLevel.WARN, entries[0].level)
        assertEquals("caution", entries[0].message)
    }

    @Test
    fun `error appends entry with ERROR level`() {
        logger.error("broken")
        val entries = logger.entries.value
        assertEquals(1, entries.size)
        assertEquals(LogLevel.ERROR, entries[0].level)
        assertEquals("broken", entries[0].message)
    }

    @Test
    fun `error with throwable includes exception message`() {
        logger.error("failed", RuntimeException("timeout"))
        val entries = logger.entries.value
        assertEquals(1, entries.size)
        assertEquals("failed: timeout", entries[0].message)
    }

    @Test
    fun `entries are ordered chronologically`() {
        logger.info("first")
        logger.info("second")
        logger.info("third")
        val entries = logger.entries.value
        assertEquals(3, entries.size)
        assertEquals("first", entries[0].message)
        assertEquals("second", entries[1].message)
        assertEquals("third", entries[2].message)
    }

    @Test
    fun `clear removes all entries`() {
        logger.info("a")
        logger.warn("b")
        logger.error("c")
        assertEquals(3, logger.entries.value.size)
        logger.clear()
        assertEquals(0, logger.entries.value.size)
    }

    @Test
    fun `entries are capped at 200`() {
        repeat(250) { i -> logger.info("msg $i") }
        val entries = logger.entries.value
        assertEquals(200, entries.size)
        // Oldest entries are dropped — first entry should be msg 50
        assertEquals("msg 50", entries[0].message)
        assertEquals("msg 249", entries[199].message)
    }

    @Test
    fun `timestamp is formatted as HH mm ss`() {
        logger.info("test")
        val ts = logger.entries.value[0].timestamp
        // Format: HH:mm:ss — 8 chars with colons at positions 2 and 5
        assertEquals(8, ts.length)
        assertEquals(':', ts[2])
        assertEquals(':', ts[5])
    }

    @Test
    fun `multiple levels interleave correctly`() {
        logger.info("i1")
        logger.error("e1")
        logger.warn("w1")
        logger.info("i2")
        val entries = logger.entries.value
        assertEquals(4, entries.size)
        assertEquals(LogLevel.INFO, entries[0].level)
        assertEquals(LogLevel.ERROR, entries[1].level)
        assertEquals(LogLevel.WARN, entries[2].level)
        assertEquals(LogLevel.INFO, entries[3].level)
    }
}
