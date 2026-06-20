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
        return effective.chunked(MAX_BLOCKS_PER_PACKET).map { chunk ->
            encodePacket(chunk.map { it.id.toInt() }, firmware.value, repeat) to chunk.sumOf { it.durationMs }
        }
    }

    companion object {
        // Two 4-bit block ids per byte, so this fits the same 16-byte payload
        // budget (19-byte packet) the protocol previously used for 16 ids.
        private const val MAX_BLOCKS_PER_PACKET = 32

        // Wire format: [firmware_version, 0x01 (Vibrate), repeat, packed_block_ids…]
        // Each payload byte packs two 4-bit block ids, high nibble first; an
        // odd-length sequence is padded with a 0x0 low nibble, which the
        // firmware treats as "no block" and skips.
        fun encodePacket(blockIds: List<Int>, firmwareVersion: Int, repeat: Int): ByteArray {
            val packed =
                blockIds.chunked(2).map { pair ->
                    val hi = pair[0]
                    val lo = pair.getOrElse(1) { 0 }
                    ((hi shl 4) or lo).toByte()
                }
            return byteArrayOf(firmwareVersion.toByte(), 0x01, repeat.toByte()) + packed.toByteArray()
        }
    }
}
