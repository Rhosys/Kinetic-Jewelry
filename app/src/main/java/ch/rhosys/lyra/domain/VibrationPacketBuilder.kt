package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.ProtocolVersion
import ch.rhosys.lyra.domain.model.VibrationMode
import javax.inject.Inject

class VibrationPacketBuilder @Inject constructor() {

    fun buildPackets(
        mode: VibrationMode,
        firmware: ProtocolVersion,
        repeat: Int = 1,
    ): List<Pair<ByteArray, Int>> {
        val blocks = mode.blocks.filter { it.since <= firmware }
        val effective = blocks.ifEmpty { VibrationMode.SHORT_PULSE.blocks }
        return effective.chunked(16).map { chunk ->
            encodePacket(chunk.map { it.id.toInt() }, firmware.value, repeat) to chunk.sumOf { it.durationMs }
        }
    }

    companion object {
        // Wire format: [firmware_version, 0x01 (Vibrate), repeat, block_ids…]
        fun encodePacket(blockIds: List<Int>, firmwareVersion: Int, repeat: Int): ByteArray =
            byteArrayOf(firmwareVersion.toByte(), 0x01, repeat.toByte()) +
                blockIds.map { it.toByte() }.toByteArray()
    }
}
