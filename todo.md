# KineticJewel — Project TODO & Context

This file is the running record of what's been built, what's pending, and
**exactly how to make each kind of change** so future sessions don't have to
rediscover the conventions.

---

## Repository map

```
Kinetic-Jewelry/
├── app/                          Android app (Kotlin, Jetpack Compose)
│   └── src/test/                 JVM unit tests — run without emulator
│       └── .../protocol/
│           └── ProtocolRoundtripTest.kt   ← Kotlin side of round-trip CI
│
├── firmware/
│   ├── protocol/                 Pure Rust crate — NO esp-idf, NO hardware
│   │   ├── src/lib.rs            ← THE file to change when the protocol changes
│   │   └── tests/
│   │       ├── fixtures/
│   │       │   └── test-vectors.json   ← canonical wire-format spec (shared)
│   │       └── roundtrip.rs      Rust side of round-trip CI
│   │
│   ├── device/                   ESP32-C3 firmware binary (esp-idf + NimBLE)
│   │   ├── src/
│   │   │   ├── main.rs           wires everything together
│   │   │   ├── ble.rs            GATT server + BLE UUIDs
│   │   │   ├── vibration.rs      step-queue thread, owns GPIO pins
│   │   │   └── config.rs         pin numbers, device name, timing
│   │   ├── .cargo/config.toml    riscv32imc-esp-espidf target + espflash runner
│   │   └── rust-toolchain.toml  nightly (stays in device/, not repo root)
│   │
│   ├── hardware/
│   │   ├── wiring.md             circuit schematic (ASCII + Mermaid diagrams)
│   │   ├── bom.md                complete parts list (prototype/breadboard)
│   │   ├── bom-production.md     SMD/miniaturized parts list w/ pogo-pad charging contacts
│   │   └── bom-charging-dock.md  separate USB-C charging base, supports 2 devices at once
│   │
│   ├── TESTING.md                three-tier test strategy (host / board / circuit)
│   └── TEST-SUITES-SPEC.md       spec for Suite A (BLE) and Suite B (round-trip)
│
└── .github/workflows/
    └── firmware.yml              CI — see "CI jobs" section below
```

---

## CI jobs

| Job | Status | What it does |
|:----|:------:|:-------------|
| `protocol-unit-tests` | ✅ live | `cargo test` in `firmware/protocol/` — 9 unit tests |
| `kotlin-roundtrip` | ✅ live | Rust `roundtrip.rs` + Kotlin `ProtocolRoundtripTest` vs shared `test-vectors.json` |
| `device-ble-integration` | 🔲 pending | Needs `firmware/device-host/` crate — see **TODO: Suite A** below |

---

## How to update the protocol

This is the main recurring task. All three steps must happen in the same PR —
CI will fail on any side that's left behind.

### Step 1 — Update the Rust parser

File: `firmware/protocol/src/lib.rs`

- `FIRMWARE_VERSION` — bump if the new format is not backwards-compatible
- `decode_block()` — add/change/remove block ID mappings
- `parse()` — change header layout, field order, new commands
- Update the unit tests at the bottom of the same file
- Run locally: `cd firmware/protocol && cargo test`

### Step 2 — Update the test vectors

File: `firmware/protocol/tests/fixtures/test-vectors.json`

This file is the **shared source of truth** read by both Rust and Kotlin CI.
Update every affected case to reflect the new byte encoding.
Add new cases for any new block types or commands.

Format reminder:
```json
{
  "id": "my-case",
  "description": "…",
  "block_ids": [1, 4, 2],
  "repeat": 1,
  "bytes_hex": "01 01 01 01 04 02",
  "decoded_blocks": [
    { "id": 1, "motor_on": true,  "duration_ms": 100 },
    { "id": 4, "motor_on": false, "duration_ms":  80 },
    { "id": 2, "motor_on": true,  "duration_ms": 250 }
  ]
}
```

`bytes_hex` layout (current placeholder protocol):
```
byte 0  — firmware version  (currently 0x01)
byte 1  — command           (0x01 = Vibrate)
byte 2  — repeat count      (1–255)
byte 3… — block IDs
```

### Step 3 — Update the Kotlin builder

File: wherever the production packet builder lives in `app/`
(currently `VibrationPacketBuilder.kt` — under redesign as of this writing)

Also update `buildPacket()` in `ProtocolRoundtripTest.kt` — this is the
independent wire-format encoder in the test that detects builder drift:

```kotlin
// app/src/test/.../protocol/ProtocolRoundtripTest.kt
private fun buildPacket(blockIds: List<Int>, repeat: Int): ByteArray {
    // keep this in sync with the real builder and with test-vectors.json
    return byteArrayOf(0x01, 0x01, repeat.toByte()) +
            blockIds.map { it.toByte() }.toByteArray()
}
```

### Step 4 — Update BLE UUIDs (if they change)

File: `ble-protocol.json` (repo root) — the single source of truth. Both the
firmware (via `firmware/protocol/build.rs`) and the Kotlin app (loaded from
packaged assets at runtime) read this one file; there's nothing else to edit.

```json
"service_uuid": "7ddac4ce-540b-46ea-a933-4be811324000",
"firmware_characteristic_uuid": "7ddac4ce-540b-46ea-a933-4be811324001",
"command_characteristic_uuid": "7ddac4ce-540b-46ea-a933-4be811324002",
```

UUIDs must also be updated in the Android app's Bluetooth controller.

### Verify

```bash
cd firmware/protocol
cargo test                   # unit tests + roundtrip
```

Kotlin tests run in CI via `./gradlew :app:testDebugUnitTest`.
To run locally: same command from the repo root (needs JDK 17).

---

## How to run the app on the emulator (local validate loop)

Single orchestrator — boots the emulator (creates it / installs the SDK on first
run), builds, installs, launches, then streams crash logs. No explicit steps.

```bash
npm run start            # debug variant
npm run start:release    # R8-minified release variant (catches stripping crashes)
```

`npm run start:release` is the local mirror of the instrumented release tests
(`testBuildType = "release"`). It installs the ProGuard/R8-processed APK signed
with the shared debug key (see `app/build.gradle.kts § buildTypes.release`) so
keep-rule / reflection crashes surface on the emulator before CI. The Play Store
upload is signed separately (`android-upload-signing.keystore` + Play App
Signing) — the local release signing config does not affect the published AAB.

AVD: `WorkspaceAVD` (pixel_7, android-35). Emulator-only helpers if needed:
`npm run setup`, `npm run emulator:create`, `npm run emulator:start`,
`npm run emulator:delete`.

---

## How to flash the firmware

Requires the ESP toolchain on your dev machine (not in this repo):

```bash
cargo install espup espflash ldproxy
espup install
. $HOME/export-esp.sh         # or ~/export-esp.sh — adds toolchain to PATH
```

Then from `firmware/device/`:

```bash
cargo run --release           # builds, flashes ESP32-C3, opens serial monitor
```

Serial output uses the `log` crate at INFO level. Look for:
```
[BOOT] KineticJewel – protocol v1
[BLE]  advertising as "KineticJewel"
[BLE]  connected (handle 0)
[BLE]  vibrate 3 block(s) × 1
```

To change target board, edit `firmware/device/.cargo/config.toml`:
- ESP32-C3: `target = "riscv32imc-esp-espidf"` (current — standard RISC-V, easy)
- ESP32/S3:  `target = "xtensa-esp32-espidf"` (needs Xtensa LLVM via `espup`)

---

## How to verify BLE without the phone app

Use **nRF Connect** (iOS/Android) or **LightBlue**:

1. Scan → connect to **KineticJewel**
2. Read characteristic `7ddac4ce-...324001` → expect `[01]`
3. Write to characteristic `7ddac4ce-...324002`:
   - `01 01 01 01` → short buzz once
   - `01 01 03 07` → click, repeated 3 times
   - `01 01 01 07 04 07` → double tap

---

## How to build the circuit

Full details in `firmware/hardware/wiring.md` and `bom.md`.

Quick reference:
- **Power**: 5× LR44 → LM2596 (trim to 5.0V) → DevKit VIN
- **Motor**: GPIO4 → 1kΩ → 2N2222 base; collector → motor(−); diode across motor
- **LED**: GPIO5 → 220Ω → LED → GND
- **Decoupling**: 100µF on 5V rail (near motor), 100nF at DevKit VCC pin
- **2N2222 pinout** (TO-92, flat face toward you): Emitter · Base · Collector

**Bring-up order** (important — don't skip):
1. Set LM2596 to 5V with no load connected (multimeter on OUT)
2. Diode-test confirms no short between 5V and GND
3. Test motor driver standalone: jumper 5V through 1kΩ to base → motor spins
4. Connect ESP32 last

---

## CI/CD conventions

### Never use `sudo` to run application code

`sudo` is only for infrastructure setup (apt-get, modprobe, systemctl, writing
root-owned config files). It must never wrap `cargo`, `gradle`, `npm`, or any
test runner — the language toolchain lives under the runner user's `$HOME` and
is invisible to root.

**Fix pattern:** grant the runner user access instead of escalating.

- **D-Bus / BlueZ**: write a policy file in `/etc/dbus-1/system.d/` that
  allows the runner user to send to `org.bluez.*`, then `sudo systemctl reload
  dbus`. Run the tests as the normal user — no sudo at test time.
  See `.github/workflows/firmware.yml § device-ble-integration` for the
  working example.
- **Device files**: `sudo chmod` or `sudo chown` the file in a setup step.
- **Privileged ports**: `sudo setcap cap_net_bind_service+ep ./binary`.
- **Group access**: `sudo usermod -a -G groupname $USER` + subshell with `sg`.

Full rule with table of patterns: `CLAUDE.md` (read by Claude Code every
session — that's what enforces this going forward).

---

## TODO

### Suite A — Device BLE integration tests ✅

Implemented in `firmware/device-host/`. CI job `device-ble-integration` is
live (no `if: false` guard). 13 test cases cover: advertise, connect, firmware
version read, single/repeat/mixed vibration, queue accumulation, unknown block
skip, version + command rejection, disconnect/re-advertise, mutex-not-held-across-sleep.

BlueZ access uses a D-Bus policy file (not `sudo cargo test`) — see CI/CD
conventions above.

---

### Kotlin protocol builder redesign ✅

`VibrationPacketBuilder` now exposes `companion object encodePacket(blockIds, firmwareVersion, repeat)`
which both `buildPackets()` (production path) and `ProtocolRoundtripTest` (CI path) call.
The test no longer has a private raw re-implementation of the wire format — any encoding
drift in the real builder will now be caught by the roundtrip CI job.

---

### BLE UUIDs — confirm or replace ✅

Replaced the old placeholder values (hand-copied across four files, which had
drifted and caused a real "cannot connect" bug) with real RFC 4122 v4 UUIDs,
generated with `uuidgen`. The version/variant fields live in the third and
fourth groups, so those (and the rest of the random bits) are left untouched;
only the last 3 hex digits — free for us to pick — are sequential for
readability: `000` for the service, then `001`, `002` for its characteristics:
- Service:         `7ddac4ce-540b-46ea-a933-4be811324000`
- Firmware char:   `7ddac4ce-540b-46ea-a933-4be811324001`
- Command char:    `7ddac4ce-540b-46ea-a933-4be811324002`

`ble-protocol.json` (repo root) is now the single source of truth: the
firmware embeds it at compile time via `firmware/protocol/build.rs`, and the
Kotlin app loads it from packaged assets at runtime via `BleProtocolIds.kt`.

---

### Physical build 🔲

All components confirmed on hand. Circuit is documented in `firmware/hardware/`.
Build once the protocol is stable enough to be worth flashing.
