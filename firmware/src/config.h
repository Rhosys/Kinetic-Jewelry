#pragma once
#include <stdint.h>

// ── BLE identity ──────────────────────────────────────────────────────────────
#define DEVICE_NAME            "KineticJewel"
#define FIRMWARE_PROTOCOL_VER  ((uint8_t)1)

// UUIDs – must match the Android app exactly
#define SERVICE_UUID           "6b2f0001-0000-1000-8000-00805f9b34fb"
#define COMMAND_CHAR_UUID      "6b2f0002-0000-1000-8000-00805f9b34fb"
// 6b2f0003 reserved for future response/notify characteristic
#define FIRMWARE_CHAR_UUID     "6b2f0004-0000-1000-8000-00805f9b34fb"

// ── Wire protocol ─────────────────────────────────────────────────────────────
// Packet layout: [version:1][cmd:1][repeat:1][blockId…]
#define PROTO_VERSION          1
#define CMD_VIBRATE            0x01

// ── Vibration block IDs ───────────────────────────────────────────────────────
#define BLOCK_SHORT_BUZZ       0x01   // 100 ms – motor on
#define BLOCK_MEDIUM_BUZZ      0x02   // 250 ms – motor on
#define BLOCK_LONG_BUZZ        0x03   // 500 ms – motor on
#define BLOCK_SHORT_PAUSE      0x04   //  80 ms – motor off
#define BLOCK_MEDIUM_PAUSE     0x05   // 200 ms – motor off
#define BLOCK_LONG_PAUSE       0x06   // 600 ms – motor off
#define BLOCK_CLICK            0x07   //  40 ms – motor on

// ── Hardware pins (ESP32-C3 defaults; override via build_flags for other boards)
#ifndef PIN_MOTOR
#  define PIN_MOTOR  4   // GPIO4 → 1 kΩ → NPN base → motor GND leg
#endif
#ifndef PIN_LED
#  define PIN_LED    5   // GPIO5 → 220 Ω → LED anode
#endif

// ── Timing ────────────────────────────────────────────────────────────────────
// After the last received packet, remain connected this long (Android may send
// a response request during this window).
#define HOLD_CONNECTED_MS      30000UL

// Hard safety disconnect if no packet is received within this time after connect.
#define IDLE_TIMEOUT_MS        60000UL
