# Testing the KineticJewel firmware & hardware

Three independent tiers. Each catches different classes of bug, and you can run
them in order from "no hardware needed" to "full assembly".

```
firmware/
  protocol/   ← Tier 1: pure logic, host-testable with `cargo test`
  device/     ← Tier 2: esp-idf binary, needs the ESP toolchain + a board
  hardware/   ← Tier 3: the circuit, validated with a multimeter
```

---

## Tier 1 — Protocol unit tests (no hardware, no ESP toolchain)

The packet-parsing logic lives in `firmware/protocol`, a dependency-free crate.
It runs on any machine with stock Rust.

```bash
cd firmware/protocol
cargo test
```

Covers: valid packets, short packets, wrong version byte, unknown command,
`repeat = 0` clamping, unknown block ids being skipped, and the full block→timing
table. **This is the layer that changes when the wire protocol is finalised** —
update `src/lib.rs`, adjust the tests, and `cargo test` proves it still parses.

This is the only tier that runs in CI or on a laptop with no board attached.

---

## Tier 2 — On-device firmware (ESP32-C3 board, no custom circuit yet)

Needs the ESP toolchain on your dev machine (not preinstalled here):

```bash
cargo install espup espflash ldproxy
espup install                       # installs the riscv esp toolchain
. $HOME/export-esp.sh               # adds it to PATH (per shell)
```

Flash and watch logs:

```bash
cd firmware/device
cargo run --release                 # builds, flashes, opens serial monitor
```

### Verify BLE without the motor circuit

Use a generic BLE central app — **nRF Connect** (Nordic, iOS/Android) or
**LightBlue** — so you're testing the firmware in isolation from your phone app:

1. Scan → confirm a device named **KineticJewel** advertises.
2. Connect → discover services → find service `7ddac4ce-540b-46ea-a933-4be811324000`.
3. **Read** the firmware characteristic `7ddac4ce-540b-46ea-a933-4be811324001` → expect one byte `0x01`.
4. **Write** to the command characteristic `7ddac4ce-540b-46ea-a933-4be811324002`:
   - `01 01 01 03` = version 1, vibrate, repeat 1, one long-buzz block.
   - Serial log should print `[BLE] vibrate 1 block(s) × 1`.

### See the pattern without a motor

Drive a bare LED (or the onboard LED) from **GPIO4** through a 220 Ω resistor.
The LED blinks the exact on/off pattern the motor will produce — confirms the
timing/queue logic end-to-end before you trust the transistor stage.

> No board on hand? The firmware can't be exercised. The protocol logic it
> depends on is still fully covered by Tier 1.

---

## Tier 3 — Circuit bring-up (multimeter, staged power-up)

**Power up in stages. Never connect the ESP32 to an unverified rail.**

### Step 1 — Set the regulator first, with no load

1. Connect the battery stack to the LM2596 input only.
2. Multimeter on the LM2596 **output**, turn the trimmer until it reads **5.00 V**.
3. Disconnect power.

### Step 2 — Continuity / short check (power off)

- Diode-test between 5 V rail and GND → should **not** read a dead short.
- Inspect for solder bridges, especially around the 2N2222 pins.

### Step 3 — Motor driver in isolation (no ESP32)

1. Motor (+) → 5 V, motor (–) → 2N2222 collector, emitter → GND.
2. Flyback diode across the motor: **cathode (banded end) → 5 V**, anode →
   collector. Confirm orientation with the meter's diode-test mode.
3. Tap a jumper from 5 V through the 1 kΩ resistor to the base → motor spins.
   Remove it → motor stops. (This mimics what GPIO4 will do.)

### Step 4 — Current sanity (meter in series with the battery)

- Motor running: ~70 mA through the driver.
- A reading of hundreds of mA or a hot transistor = wiring fault, power down.

### Step 5 — Connect the ESP32 last

1. 5 V → DevKit VIN, GND → GND. Decoupling caps in place (100 µF on the rail,
   100 nF at the DevKit VCC pin).
2. Power up, run the firmware (Tier 2), drive it from nRF Connect.
3. Motor + LED should fire together on each vibrate write.

### Transistor pinout reminder

2N2222 in TO-92, **flat face toward you**: Emitter (left), Base (middle),
Collector (right).
