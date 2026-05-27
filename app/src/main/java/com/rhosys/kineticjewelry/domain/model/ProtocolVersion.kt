package com.rhosys.kineticjewelry.domain.model

enum class ProtocolVersion(val value: Int) {
    V1(1);

    companion object {
        fun fromInt(v: Int): ProtocolVersion = entries.firstOrNull { it.value == v } ?: V1
    }
}
