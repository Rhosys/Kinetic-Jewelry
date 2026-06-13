package ch.rhosys.lyra.protocol

import ch.rhosys.lyra.domain.VibrationPacketBuilder
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

/**
 * Reads the canonical test-vectors.json and verifies that
 * [VibrationPacketBuilder.encodePacket] produces byte-for-byte identical output
 * for every case.
 *
 * The vectors file lives in firmware/protocol/tests/fixtures/test-vectors.json
 * (one copy, shared by Rust and Kotlin CI).  The Gradle task in build.gradle.kts
 * passes its absolute path via the system property "protocolVectorsPath".
 *
 * When the protocol changes:
 *   1. Update test-vectors.json
 *   2. Update VibrationPacketBuilder.encodePacket() in the same PR
 * CI will fail on any side that is not updated.
 */
class ProtocolRoundtripTest {

    @Test
    fun `all test vectors produce canonical bytes`() {
        val path = System.getProperty("protocolVectorsPath")
            ?: error("protocolVectorsPath system property not set — check app/build.gradle.kts")

        val root            = JSONObject(File(path).readText())
        val firmwareVersion = root.getInt("firmware_version")
        val cases           = root.getJSONArray("cases")

        repeat(cases.length()) { i ->
            val case     = cases.getJSONObject(i)
            val id       = case.getString("id")
            val repeat   = case.getInt("repeat")
            val blockIds = case.getJSONArray("block_ids").let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            }
            val bytesHex = case.getString("bytes_hex")
            val expected = bytesHex.split(" ")
                .map { it.toInt(16).toByte() }
                .toByteArray()

            val actual = VibrationPacketBuilder.encodePacket(blockIds, firmwareVersion, repeat)

            assertArrayEquals(
                "case '$id': byte mismatch\n  expected: $bytesHex\n  actual:   ${actual.toHex()}",
                expected,
                actual,
            )
        }
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02x".format(it) }
}
