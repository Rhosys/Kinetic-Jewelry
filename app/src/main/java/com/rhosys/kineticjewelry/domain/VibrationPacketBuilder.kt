package com.rhosys.kineticjewelry.domain

import com.rhosys.kineticjewelry.domain.model.ProtocolVersion
import com.rhosys.kineticjewelry.domain.model.VibrationMode

class VibrationPacketBuilder {

    fun buildPackets(
        mode: VibrationMode,
        firmware: ProtocolVersion,
        repeat: Int = 1,
    ): List<Pair<ByteArray, Int>> {
        val blocks = mode.blocks.filter { it.since <= firmware }
        val effective = blocks.ifEmpty { VibrationMode.SHORT_PULSE.blocks }
        return effective.chunked(16).map { chunk ->
            val bytes = byteArrayOf(firmware.value.toByte(), 0x01, repeat.toByte()) +
                chunk.map { it.id }.toByteArray()
            bytes to chunk.sumOf { it.durationMs }
        }
    }
}
