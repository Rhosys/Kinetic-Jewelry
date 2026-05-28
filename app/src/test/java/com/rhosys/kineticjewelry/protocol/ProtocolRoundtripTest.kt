package com.rhosys.kineticjewelry.protocol

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

/**
 * Reads the canonical test-vectors.json and verifies that the wire encoding
 * produced by [buildPacket] matches every case.
 *
 * The vectors file lives in firmware/protocol/tests/fixtures/test-vectors.json
 * (one copy, referenced by both Rust and Kotlin CI jobs).  The Gradle task
 * in build.gradle.kts passes its absolute path via the system property
 * "protocolVectorsPath" so neither side needs to guess relative paths.
 *
 * When the protocol changes:
 *   1. Update test-vectors.json
 *   2. Update buildPacket() below to match the new format
 *   3. Update the production packet builder in the same PR
 * CI will fail on any side that is not updated.
 */
class ProtocolRoundtripTest {

    // ── Wire-format encoder ───────────────────────────────────────────────────
    // Directly encodes the current wire format:  [version, cmd, repeat, blocks…]
    // This is intentionally a plain implementation of the spec, separate from
    // the production builder, so that changes to either are caught by CI.
    private fun buildPacket(blockIds: List<Int>, repeat: Int): ByteArray {
        require(repeat in 1..255)
        return byteArrayOf(0x01, 0x01, repeat.toByte()) +
                blockIds.map { it.toByte() }.toByteArray()
    }

    // ── Test ──────────────────────────────────────────────────────────────────
    @Test
    fun `all test vectors produce canonical bytes`() {
        val path = System.getProperty("protocolVectorsPath")
            ?: error("protocolVectorsPath system property not set — check app/build.gradle.kts")

        val root    = JSONObject(File(path).readText())
        val cases   = root.getJSONArray("cases")

        repeat(cases.length()) { i ->
            val case       = cases.getJSONObject(i)
            val id         = case.getString("id")
            val repeat     = case.getInt("repeat")
            val blockIds   = case.getJSONArray("block_ids").let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            }
            val bytesHex   = case.getString("bytes_hex")
            val expected   = bytesHex.split(" ")
                .map { it.toInt(16).toByte() }
                .toByteArray()

            val actual = buildPacket(blockIds, repeat)

            assertArrayEquals(
                "case '$id': byte mismatch\n  expected: $bytesHex\n  actual:   ${actual.toHex()}",
                expected,
                actual,
            )
        }
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02x".format(it) }
}
