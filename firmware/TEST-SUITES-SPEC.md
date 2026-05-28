# Test Suites Specification

Two test suites to be implemented once their prerequisites are met.
The GitHub workflow (`.github/workflows/firmware.yml`) already contains
stub jobs for both; remove the `if: false` guard when each suite is ready.

---

## Context

```
firmware/
  protocol/       pure Rust, host-testable — unit tests running today
  device/         esp-idf binary — only runs on ESP32-C3 hardware
  device-host/    (Suite A) native Linux version for CI integration testing
```

```
app/              Android / Kotlin — unit tests runnable on host JVM today
firmware/protocol/tests/fixtures/test-vectors.json   (Suite B, to be created)
```

---

## Suite A — Device BLE integration tests

### Goal

Validate the full firmware stack — BLE server, protocol parsing, and vibration
sequencing — against a software BLE central, without physical hardware or an
ESP32 board.

### Why this matters

Tier 1 (`cargo test` in `firmware/protocol`) tests `parse()` in isolation.
Suite A tests everything on top of it: does the GATT server correctly receive
writes? Does it feed raw bytes to `parse()`? Does the vibration queue execute
the right GPIO pattern? These paths are currently untested in CI.

---

### Architecture

```
┌──────────────────────────────────────────┐
│  CI runner (Ubuntu)                       │
│                                           │
│  ┌──────────────────────┐                │
│  │  device-host binary   │  peripheral   │
│  │  (GATT server)        │◄─────────────►│
│  │  uses bluer + MockGPIO│               │
│  └──────────────────────┘               │
│            ▲  virtual HCI (hci_vhci)     │
│  ┌──────────────────────┐               │
│  │  integration test     │  central      │
│  │  (bluer GATT client)  │──────────────►│
│  └──────────────────────┘               │
└──────────────────────────────────────────┘
```

The `device-host` binary is a native-target sibling of `firmware/device` that
swaps out two platform-specific dependencies:

| Layer     | firmware/device (ESP32)  | firmware/device-host (Linux CI) |
|:----------|:-------------------------|:--------------------------------|
| BLE stack | `esp32-nimble`            | `bluer` (official BlueZ Rust)   |
| GPIO      | `esp-idf-hal PinDriver`  | `MockGpio` (records calls)      |
| Protocol  | `kinetic-protocol` crate | same crate, unchanged           |

---

### New files to create

#### `firmware/device-host/Cargo.toml`

```toml
[package]
name    = "kinetic-jewel-host"
version = "0.1.0"
edition = "2021"

[dependencies]
kinetic-protocol = { path = "../protocol" }
bluer    = { version = "0.17", features = ["full"] }
tokio    = { version = "1",    features = ["full"] }
anyhow   = "1"
log      = "0.4"
env_logger = "0.11"

[dev-dependencies]
tokio-test = "0.4"
```

#### `firmware/device-host/src/mock_gpio.rs`

A thread-safe record of every `(timestamp, level)` call made on each pin:

```rust
pub struct MockGpio {
    history: Arc<Mutex<Vec<(Instant, bool)>>>,
}

impl MockGpio {
    pub fn new() -> Self { ... }
    pub fn set(&self, on: bool) { self.history.lock().unwrap().push((Instant::now(), on)); }
    pub fn events(&self) -> Vec<(Instant, bool)> { self.history.lock().unwrap().clone() }
}
```

#### `firmware/device-host/src/ble_peripheral.rs`

Wraps `bluer::gatt::local::*` to expose the same service + characteristic
layout as `firmware/device/src/ble.rs`. On each write to the command
characteristic, calls `kinetic_protocol::parse()` and forwards the result to
the vibration thread exactly as the esp-idf version does.

#### `firmware/device-host/tests/ble_integration.rs`

The integration test binary. Uses `bluer` as a GATT central. Spawns the
peripheral in a Tokio task, then exercises it as a BLE client.

---

### CI prerequisites

Add these steps to the workflow job (already stubbed):

```yaml
- name: Install BlueZ
  run: |
    sudo apt-get update -qq
    sudo apt-get install -y bluez

- name: Load virtual HCI kernel module
  run: sudo modprobe hci_vhci
```

The `hci_vhci` module creates a virtual `hci0` adapter that both the peripheral
and the central can use without any physical Bluetooth hardware.

---

### Test cases

Each test follows the pattern: start peripheral → central connects → exercise →
assert `MockGpio.events()` or `MockGpio.motor_pin.events()`.

| # | Name | Action | Assert |
|:--|:-----|:-------|:-------|
| 1 | **Advertises** | central scans | "KineticJewel" found with service UUID `6b2f0001-…` |
| 2 | **Connects** | central connects | no error; GATT service discoverable |
| 3 | **Firmware version** | read char `6b2f0004` | value = `[0x01]` |
| 4 | **Single block** | write `[01 01 01 01]` (short buzz ×1) | motor GPIO high for ~100 ms then low |
| 5 | **Repeat** | write `[01 01 03 01]` (short buzz ×3) | motor high × 3 with correct durations |
| 6 | **Pause block** | write `[01 01 01 04]` (short pause ×1) | motor stays low throughout |
| 7 | **Mixed sequence** | write `[01 01 01 01 04 03]` (buzz, pause, long buzz) | GPIO pattern matches `on-100ms / off-80ms / on-500ms` |
| 8 | **Queue accumulation** | write two packets in rapid succession | both patterns execute in order; second starts after first finishes |
| 9 | **Unknown block IDs skipped** | write `[01 01 01 01 FF 03]` | GPIO pattern = short buzz + long buzz (0xFF ignored) |
| 10 | **Wrong version rejected** | write `[02 01 01 01]` | no GPIO activity; device stays connected |
| 11 | **Unknown command rejected** | write `[01 42 01 01]` | no GPIO activity; device stays connected |
| 12 | **Disconnect → re-advertise** | central disconnects | device becomes discoverable again within 2 s |
| 13 | **Mutex not held across sleep** | write, immediately write again | second write accepted without blocking; no deadlock |

---

### Timing assertion strategy

`MockGpio.events()` returns `Vec<(Instant, bool)>`. Assert that the duration
between consecutive events matches the block's expected duration within a
±20 ms tolerance (to account for thread scheduling jitter).

```rust
fn assert_duration(events: &[(Instant, bool)], idx: usize, expected_ms: u64) {
    let delta = events[idx + 1].0.duration_since(events[idx].0);
    let diff = (delta.as_millis() as i64 - expected_ms as i64).abs();
    assert!(diff < 20, "block {idx}: expected ~{expected_ms}ms, got {delta:?}");
}
```

---

## Suite B — Kotlin ↔ Rust protocol round-trip

### Goal

Prove that a packet built by the Kotlin protocol layer and a packet parsed by
the Rust protocol layer agree on byte-level encoding — without any BLE stack,
emulator, or physical device.

### Why this matters

The Kotlin app and the Rust firmware are developed independently.  Without a
cross-language check it is possible for each side to pass its own unit tests
while silently disagreeing on what `repeat=3, blocks=[SHORT_BUZZ, MEDIUM_PAUSE]`
looks like as bytes.  This suite catches that divergence automatically on every
PR that touches either side.

---

### Architecture

A single JSON file is the shared source of truth.  Both test suites read it
independently.  Neither side needs to invoke the other; they just both agree
on the same golden data.

```
firmware/protocol/tests/fixtures/test-vectors.json
            │
            ├── read by ──► firmware/protocol/tests/roundtrip.rs  (Rust, cargo test)
            │
            └── read by ──► app/src/test/.../ProtocolRoundtripTest.kt  (Kotlin, ./gradlew test)
```

---

### Test vector file

**Path:** `firmware/protocol/tests/fixtures/test-vectors.json`

> **Write this file after the protocol is finalised.**
> The structure is specified here; the values come from the agreed spec.

```jsonc
{
  "firmware_version": 1,
  "description": "Authoritative wire-format test vectors. Both the Kotlin builder and the Rust parser must agree on every case.",
  "cases": [
    {
      "id": "single-short-buzz",
      "description": "Simplest possible vibrate command",
      "repeat": 1,
      "block_ids": [1],
      "bytes_hex": "01 01 01 01",
      "decoded_blocks": [
        { "id": 1, "motor_on": true, "duration_ms": 100 }
      ]
    },
    {
      "id": "buzz-pause-buzz",
      "description": "Alternating on/off — validates pause block encoding",
      "repeat": 1,
      "block_ids": [1, 4, 1],
      "bytes_hex": "01 01 01 01 04 01",
      "decoded_blocks": [
        { "id": 1, "motor_on": true,  "duration_ms": 100 },
        { "id": 4, "motor_on": false, "duration_ms":  80 },
        { "id": 1, "motor_on": true,  "duration_ms": 100 }
      ]
    },
    {
      "id": "all-block-types",
      "description": "One of every known block ID",
      "repeat": 1,
      "block_ids": [1, 2, 3, 4, 5, 6, 7],
      "bytes_hex": "01 01 01 01 02 03 04 05 06 07",
      "decoded_blocks": [
        { "id": 1, "motor_on": true,  "duration_ms": 100 },
        { "id": 2, "motor_on": true,  "duration_ms": 250 },
        { "id": 3, "motor_on": true,  "duration_ms": 500 },
        { "id": 4, "motor_on": false, "duration_ms":  80 },
        { "id": 5, "motor_on": false, "duration_ms": 200 },
        { "id": 6, "motor_on": false, "duration_ms": 600 },
        { "id": 7, "motor_on": true,  "duration_ms":  40 }
      ]
    },
    {
      "id": "repeat-three",
      "description": "Repeat count is encoded in byte 2",
      "repeat": 3,
      "block_ids": [7],
      "bytes_hex": "01 01 03 07",
      "decoded_blocks": [
        { "id": 7, "motor_on": true, "duration_ms": 40 }
      ]
    }
  ]
}
```

---

### Rust side — `firmware/protocol/tests/roundtrip.rs`

New integration test file (runs via `cargo test --test roundtrip`).

```rust
// Reads test-vectors.json and verifies protocol::parse() agrees with each case.

use kinetic_protocol::{parse, Command, VibBlock};
use std::fs;

#[derive(serde::Deserialize)]
struct Vectors { cases: Vec<Case> }

#[derive(serde::Deserialize)]
struct Case {
    id: String,
    bytes_hex: String,
    repeat: u8,
    decoded_blocks: Vec<BlockSpec>,
}

#[derive(serde::Deserialize)]
struct BlockSpec { motor_on: bool, duration_ms: u64 }

#[test]
fn all_vectors_parse_correctly() {
    let json = fs::read_to_string(
        concat!(env!("CARGO_MANIFEST_DIR"), "/tests/fixtures/test-vectors.json")
    ).expect("test-vectors.json missing");

    let vectors: Vectors = serde_json::from_str(&json).unwrap();

    for case in &vectors.cases {
        let bytes: Vec<u8> = case.bytes_hex.split_whitespace()
            .map(|h| u8::from_str_radix(h, 16).unwrap())
            .collect();

        let cmd = parse(&bytes).unwrap_or_else(|e| {
            panic!("case '{}': parse failed: {:?}", case.id, e)
        });

        let Command::Vibrate { blocks, repeat } = cmd;

        assert_eq!(repeat, case.repeat, "case '{}': wrong repeat", case.id);
        assert_eq!(blocks.len(), case.decoded_blocks.len(),
                   "case '{}': wrong block count", case.id);

        for (i, (got, want)) in blocks.iter().zip(&case.decoded_blocks).enumerate() {
            assert_eq!(got.motor_on,    want.motor_on,    "case '{}' block {i}: motor_on", case.id);
            assert_eq!(got.duration_ms, want.duration_ms, "case '{}' block {i}: duration", case.id);
        }
    }
}
```

Add `serde` + `serde_json` to `firmware/protocol/Cargo.toml` dev-dependencies.

---

### Kotlin side — `app/src/test/.../ProtocolRoundtripTest.kt`

New JUnit test class. Reads the same JSON from the classpath (copy or symlink
the file into `app/src/test/resources/`).

Responsibilities:
- For each case in `test-vectors.json`, call the Kotlin packet builder with the
  given `block_ids` and `repeat`.
- Assert the output bytes (as a hex string) equal `bytes_hex`.
- The test does **not** test parsing — that is the Rust side's job. It only
  asserts that the Kotlin builder produces the canonical encoding.

```kotlin
class ProtocolRoundtripTest {

    @Test
    fun `all test vectors produce canonical bytes`() {
        val json = javaClass.getResourceAsStream("/test-vectors.json")!!
            .bufferedReader().readText()

        val vectors = Json.decodeFromString<TestVectors>(json)
        val builder = ProtocolPacketBuilder()   // the new protocol builder, TBD

        for (case in vectors.cases) {
            val expectedBytes = case.bytesHex.split(" ")
                .map { it.toInt(16).toByte() }.toByteArray()

            val actualBytes = builder.buildPacket(
                blockIds = case.blockIds,
                repeat   = case.repeat,
            )

            assertArrayEquals(
                "case '${case.id}': byte mismatch",
                expectedBytes,
                actualBytes,
            )
        }
    }
}
```

> Note: `ProtocolPacketBuilder` is the replacement for the current
> `VibrationPacketBuilder` after the protocol redesign. The interface above is
> illustrative; adapt it to whatever the final Kotlin builder looks like.

---

### Prerequisites before implementing Suite B

1. **Protocol finalised** — `bytes_hex` values in the test vectors must reflect
   the agreed wire format.  Do not write the vectors file against the
   placeholder format in `protocol.rs`; it will change.

2. **Kotlin builder updated** — `VibrationPacketBuilder` (or its replacement)
   must be updated to match the finalised protocol.  The existing tests in
   `VibrationPacketBuilderTest.kt` will need updating too.

3. **Shared resource** — `test-vectors.json` must be accessible to both test
   runners.  Recommended: canonical location is
   `firmware/protocol/tests/fixtures/test-vectors.json` (owned by the protocol
   spec); the Kotlin test copies or symlinks it via a Gradle `processTestResources`
   task so it lands on the JVM classpath.

---

## Summary: what unblocks each suite

| Suite | Status | Unblocked when |
|:------|:------:|:---------------|
| A – Device BLE integration | pending | `firmware/device-host/` crate exists; `bluer`-based peripheral + `MockGpio` implemented |
| B – Kotlin ↔ Rust round-trip | **live** | ✓ `test-vectors.json` written; `roundtrip.rs` + `ProtocolRoundtripTest.kt` implemented |
