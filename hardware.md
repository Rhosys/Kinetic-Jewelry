# KineticJewel – Physical Hardware Build Guide

This guide walks you from an empty breadboard to a working, firmware-flashed device
you can operate over BLE. Follow every step in order and use the verification checks
before moving to the next stage.

---

## What you need

### Parts (all confirmed on hand – see `firmware/hardware/bom.md`)

| Ref  | Part               | Value / Spec                      |
|:-----|:-------------------|:----------------------------------|
| U1   | ESP32-C3 DevKit    | USB-CDC, 5V tolerant VIN          |
| VR1  | LM2596 module      | Adjustable buck converter          |
| Q1   | 2N2222 NPN         | TO-92 package                     |
| D1   | Diode              | 1N4001 or similar (any ≥1A)       |
| M1   | ERM coin motor     | 3–5V, ~70 mA                      |
| LED1 | LED                | Any colour, V_f ≈ 2.0V            |
| BT1  | 5× LR44 cells      | 1.5V each = 7.5V total in series  |
| R1   | 1 kΩ resistor      | ¼W, brown-black-red               |
| R2   | 220 Ω resistor     | ¼W, red-red-brown                 |
| C1   | 100 µF electrolytic| 10V or higher rating              |
| C2   | 100 nF ceramic     | Marked "104"                      |

### Tools

- **Multimeter** — essential; you will use it at every stage
- Full-size or half-size breadboard
- Jumper wires (male-male and male-female)
- 5× LR44 battery holder (or tape + bare wire leads)
- USB-A to USB-C cable (for flashing)
- Laptop / desktop running Linux, macOS, or Windows
- Fine-tip tweezers or small needle-nose pliers for TO-92 legs

### Software (install before the hardware session)

```
# Rust nightly + ESP target
rustup toolchain install nightly
rustup component add rust-src --toolchain nightly

# espflash for flashing and monitoring
cargo install espflash

# nRF Connect (phone) – for manual BLE testing
# iOS: https://apps.apple.com/app/nrf-connect-for-mobile/id1054362403
# Android: https://play.google.com/store/apps/details?id=no.nordicsemi.android.mcp
```

---

## Safety notes

| Risk | Mitigation |
|:-----|:-----------|
| 7.5V battery polarity | Always verify + and − before connecting. Reversed polarity destroys the LM2596. |
| LM2596 at wrong voltage | Calibrate the trimmer FIRST, before connecting any load. |
| Electrolytic capacitor polarity | Longer leg = +. Reversed caps can rupture. |
| ESD on ESP32 | Touch a grounded metal surface (e.g. PC chassis) before handling the DevKit. |
| Motor back-EMF | The flyback diode (D1) is mandatory. Without it the 2N2222 will eventually fail. |
| Hot transistor | If Q1 gets too hot to touch within seconds, power off immediately. |

---

## Build sequence overview

```
Stage 1 → LM2596 calibration     (isolated, no load)
Stage 2 → Power rail assembly    (capacitor + ESP32)
Stage 3 → Motor driver circuit   (transistor + motor + flyback diode)
Stage 4 → LED indicator circuit
Stage 5 → Full power-on test
Stage 6 → Firmware flash
Stage 7 → BLE functional test
Stage 8 → Full vibration test
```

**Rule:** complete and verify each stage before starting the next.
Disconnect the batteries whenever you are changing wiring.

---

## Stage 1 — LM2596 Calibration

> **Do this before anything else.** Setting 5 V with no load attached
> ensures you never accidentally apply the wrong voltage to the ESP32 or motor.

### 1.1 Connect batteries to LM2596 only

```
5× LR44 (+) ──────► LM2596 IN+
5× LR44 (−) ──────► LM2596 IN−
```

Nothing else is connected to the LM2596 output yet.

### 1.2 Measure input voltage

- Multimeter probes on LM2596 IN+ (red) and IN− (black)
- Expected: **7.0 – 7.5 V** with fresh cells
- If < 6 V: cells are depleted. Replace all five before continuing.

### 1.3 Adjust output to 5.0 V

- Multimeter probes on LM2596 OUT+ (red) and OUT− (black)
- Turn the blue trimmer pot slowly with a small screwdriver
  - Clockwise typically raises output voltage (varies by module — check direction first)
- Adjust until the display reads **5.00 V ± 0.05 V**
- Wait 10 seconds and recheck. The reading must be stable.

### 1.4 Disconnect batteries

Stage 1 complete. The LM2596 is calibrated and will not be touched again.

---

## Stage 2 — Power Rail Assembly

### 2.1 Breadboard layout

Place components with these guidelines:
- LM2596 module: left end of breadboard, wired into power rails
- ESP32-C3 DevKit: centre, straddling the channel gap
- Motor driver: right side of breadboard
- LED: far right

### 2.2 Wire the 5 V power rail

```
LM2596 OUT+  ──────► Breadboard + rail (red)
LM2596 OUT−  ──────► Breadboard − rail (black)  ← this is your GND throughout
```

### 2.3 Add C1 (100 µF bulk decoupling)

Place C1 bridging the two power rails, physically near where the motor will sit.

```
C1 (+) leg (longer) → 5 V rail
C1 (−) leg (shorter) → GND rail
```

> C1 absorbs the current surge when the motor starts. Without it the 5 V rail
> will sag and the ESP32 may reset.

### 2.4 Connect ESP32-C3 DevKit

- DevKit **VIN** pin → 5 V rail
- DevKit **GND** pin → GND rail (use the GND pin closest to VIN)

### 2.5 Add C2 (100 nF high-frequency decoupling)

Place C2 as close as physically possible to the DevKit VCC/3V3 pin.
Ceramic caps have no polarity.

```
C2: between DevKit 3V3 pin and GND rail
```

### 2.6 Power-on verification — Stage 2

Reconnect batteries, then measure:

| Point | Expected | Measured | Pass? |
|:------|:---------|:---------|:-----:|
| LM2596 OUT+ to GND | 5.0 V ± 0.1 V | | ☐ |
| DevKit VIN to GND | 5.0 V ± 0.1 V | | ☐ |
| DevKit 3V3 pin to GND | 3.3 V ± 0.1 V | | ☐ |
| DevKit power LED | lit | | ☐ |

If the DevKit 3.3 V pin reads correctly, the onboard regulator is working.
Disconnect batteries before Stage 3.

---

## Stage 3 — Motor Driver Circuit

The 2N2222 switches the motor's ground return. The flyback diode suppresses the
inductive spike when the motor is switched off.

### 3.1 Identify the 2N2222 pin-out

Hold the transistor with the **flat face toward you** and legs pointing down:

```
  FLAT FACE
 ┌──────────┐
 │  2N2222  │
 └──┬──┬──┬─┘
    E  B  C
  Emitter  Base  Collector
  (left) (middle) (right)
```

### 3.2 Place the transistor

Insert Q1 into three adjacent rows on the breadboard so no two legs share a row.
Keep track of which row is E, B, and C.

### 3.3 Wire Emitter to GND

```
Q1 Emitter (left leg) → GND rail
```

### 3.4 Wire the base resistor

```
DevKit GPIO4 ──[R1 1kΩ]──► Q1 Base (middle leg)
```

R1 limits base current to ≈ 2.6 mA — enough to fully saturate Q1 at 70 mA motor load.

### 3.5 Wire the motor

```
5 V rail ──────────────────────► Motor (+) wire
Motor (−) wire ────────────────► Q1 Collector (right leg)
```

### 3.6 Wire the flyback diode

The diode band (stripe) marks the **cathode**.

```
D1 cathode (band) → 5 V rail    (same node as Motor (+))
D1 anode          → Q1 Collector (same node as Motor (−))
```

The diode sits in parallel with the motor. When Q1 turns off and the motor
coil tries to maintain current, the diode provides a safe recirculation path
instead of spiking the collector voltage into Q1.

### 3.7 Pre-power wiring checklist

Verify every connection with the multimeter continuity function before
applying power:

| Connection | ✓ |
|:-----------|:-:|
| GPIO4 → R1 → Q1 Base | ☐ |
| Q1 Emitter → GND rail | ☐ |
| Q1 Collector → Motor (−) wire | ☐ |
| Motor (+) wire → 5 V rail | ☐ |
| D1 cathode (banded end) → 5 V rail | ☐ |
| D1 anode → Q1 Collector / Motor (−) | ☐ |
| No short between 5 V rail and GND rail | ☐ |

### 3.8 Manual motor test

Reconnect batteries. Use a short jumper wire:

1. Briefly touch one end of the jumper to GPIO4's row on the breadboard and
   the other end to the DevKit 3V3 pin (3.3 V).
2. The motor should vibrate / spin.
3. Remove the jumper. The motor should stop within a few milliseconds.

Expected results:
- Motor runs: transistor is correctly wired and motor is healthy.
- Motor does not run: re-check transistor orientation. The most common mistake is
  swapping Emitter and Collector (Q1 will often not saturate if wired backwards).
- Transistor gets very hot within a few seconds: short circuit — power off immediately
  and re-check that Emitter goes to GND and Collector goes to the motor, not the reverse.

Disconnect batteries after the test.

---

## Stage 4 — LED Indicator Circuit

### 4.1 Identify LED polarity

- **Long leg** = Anode (+)
- **Short leg** = Cathode (−), also on the flat side of the plastic package

### 4.2 Wire the LED

```
DevKit GPIO5 ──[R2 220Ω]──► LED Anode (long leg)
                              LED Cathode (short leg) ──► GND rail
```

R2 sets LED current to ≈ 6 mA — bright enough to see, well within GPIO limits.

### 4.3 Manual LED test

Reconnect batteries. Momentarily bridge GPIO5 to DevKit 3V3.
LED should light. Remove jumper; LED goes off.

If the LED does not light but the voltage across R2 is ≈ 3.3 V:
the LED is wired backwards. Flip it.

Disconnect batteries.

---

## Stage 5 — Full Power-On Test (no firmware)

Reconnect batteries and perform all measurements:

| Measurement | Expected | Pass? |
|:------------|:---------|:-----:|
| 5 V rail to GND | 5.0 V ± 0.1 V | ☐ |
| DevKit 3V3 to GND | 3.3 V ± 0.1 V | ☐ |
| GPIO4 to GND (idle) | < 0.1 V | ☐ |
| GPIO5 to GND (idle) | < 0.1 V | ☐ |
| Q1 Collector to GND | 5.0 V (motor off, no pull-down) | ☐ |
| D1 forward voltage (anode−cathode) | reversed ≈ –5 V when motor off | ☐ |

The board is ready for firmware.

---

## Stage 6 — Firmware Build and Flash

### 6.1 Set up the Rust toolchain (first time only)

```bash
# From the repo root:
cd firmware/device

# Install the nightly toolchain and RISC-V target
rustup toolchain install nightly
rustup component add rust-src --toolchain nightly
rustup target add riscv32imc-unknown-none-elf --toolchain nightly

# Install espflash
cargo install espflash
```

### 6.2 Connect the DevKit via USB

Plug the USB-C cable into the DevKit and the other end into your computer.
On Linux: a `/dev/ttyUSBx` or `/dev/ttyACM0` device appears.
On macOS: `/dev/cu.usbmodemXXXX`.
On Windows: a COM port appears in Device Manager.

To confirm:

```bash
espflash board-info
# Expected: ESP32-C3, flash size, MAC address
```

### 6.3 Build and flash

```bash
cd firmware/device
cargo build --release
espflash flash --release --monitor
```

`--monitor` opens a serial console at 115 200 baud immediately after flashing.
You should see:

```
I (xxx) boot: ESP-IDF vX.X.X
...
I (xxx) kinetic-jewel: [BLE] advertising as "KineticJewel"
```

If you see a panic or reset loop, note the backtrace and consult the
Troubleshooting section at the end of this guide.

### 6.4 Exit the monitor

Press `Ctrl-R` to reset the device, `Ctrl-C` to exit the monitor.

---

## Stage 7 — BLE Functional Test (nRF Connect)

### 7.1 Scan for the device

1. Open nRF Connect on your phone.
2. Tap **Scanner** → **Scan**.
3. Look for "KineticJewel" in the list.
4. If it does not appear within 10 s: check serial monitor output; the device
   may have crashed. Re-flash if necessary.

### 7.2 Connect

Tap "KineticJewel" → **Connect**.
The serial monitor should print:

```
[BLE] connected (handle X)
```

### 7.3 Discover services

In nRF Connect, tap the device → **Services**.
You should see one service:

```
Service: 7ddac4ce-540b-46ea-a933-4be811324000
  Characteristic: 7ddac4ce-...324001  [READ]
  Characteristic: 7ddac4ce-...324002  [WRITE]
```

### 7.4 Read the firmware version

Tap the 7ddac4ce-...324001 characteristic → read icon (↓).
Expected value: `01` (protocol version 1).

### 7.5 Send a vibration command

Tap the 7ddac4ce-...324002 characteristic → write icon (↑).
Select "Byte array" and enter:

```
01 01 01 01
```

This is: version=1, command=vibrate, repeat=1, block=short_buzz (100 ms).

The motor should vibrate for ~100 ms and the LED should flash simultaneously.
The serial monitor should print:

```
[BLE] vibrate 1 block(s) × 1
```

### 7.6 More test patterns

| Hex bytes | Expected behaviour |
|:----------|:-------------------|
| `01 01 01 01` | Short buzz × 1 (~100 ms) |
| `01 01 03 01` | Short buzz × 3 (~300 ms total) |
| `01 01 01 01 04 03` | Short buzz → short pause → long buzz |
| `01 01 01 03` | Long buzz × 1 (~500 ms) |
| `02 01 01 01` | Rejected (wrong version) — no vibration |
| `01 42 01 01` | Rejected (unknown command) — no vibration |

### 7.7 Disconnect test

Tap Disconnect in nRF Connect.
Serial monitor:

```
[BLE] disconnected (handle X reason 0)
```

Wait 2 s, then re-scan. "KineticJewel" should reappear, confirming the device
automatically restarts advertising after disconnect.

---

## Stage 8 — Full Vibration Test

Test the complete motor + LED behaviour as a system:

| Test | Command bytes | Expected | Observed | ✓ |
|:-----|:-------------|:---------|:---------|:-:|
| Single buzz | `01 01 01 01` | Motor on ~100 ms, LED on ~100 ms | | ☐ |
| Triple repeat | `01 01 03 07` | Motor pulses 3 × 40 ms (click × 3) | | ☐ |
| Long buzz | `01 01 01 03` | Motor on ~500 ms | | ☐ |
| Pause only | `01 01 01 04` | No motor movement, ~80 ms | | ☐ |
| Pattern: buzz-pause-buzz | `01 01 01 01 04 03` | 100 ms on, 80 ms off, 500 ms on | | ☐ |
| Rapid double write | Write twice in < 50 ms | Both patterns execute in order | | ☐ |

---

## Troubleshooting

### Device does not appear in BLE scan

1. Serial monitor shows "advertising as KineticJewel": the device is running but
   your phone may be filtering. Disable "only show nearby" in nRF Connect.
2. No serial output after flash: the device may be stuck in boot loop.
   Hold BOOT button while pressing RESET to enter download mode, then reflash.
3. espflash cannot find the device: check USB cable and `/dev/ttyACM0` permissions
   (`sudo usermod -a -G dialout $USER` on Linux, then log out and back in).

### Motor does not vibrate after BLE write

1. Serial shows "parse error": the hex you entered is malformed. Re-check the byte order.
2. Serial shows "vibrate N block(s) × R" but motor silent:
   - Measure GPIO4 with multimeter while sending command: should pulse 3.3 V.
   - If GPIO4 pulses but motor does not move: check Q1 orientation and motor wiring.
   - If GPIO4 does not pulse: firmware GPIO assignment. Check `firmware/device/src/config.rs`.

### Motor runs but never stops

The firmware logic always turns the motor off after each step. If it runs
continuously, the most likely cause is a stuck queue or the vibration thread
holding the mutex across a sleep. Pull the batteries to stop it, then
check `firmware/device/src/vibration.rs` — the `pop_front()` must happen
before any `thread::sleep()`.

### LED does not light

- Voltage across R2 ≈ 0 V: GPIO5 not driving high → firmware issue.
- Voltage across R2 ≈ 3 V but LED off: LED polarity reversed (flip it).
- LED lights but motor does not: GPIO5 and GPIO4 may be swapped.
  Check `firmware/device/src/config.rs` PIN_LED and PIN_MOTOR values.

### ESP32 resets when motor starts

The 5 V rail is drooping under motor load.
- Verify C1 (100 µF) is present and has correct polarity.
- Measure 5 V rail voltage the instant motor starts (oscilloscope or fast multimeter).
- If voltage drops below 4.5 V: replace LR44 cells or add a second 100 µF capacitor.

### LM2596 output drifts or oscillates

- Check input voltage: if below 6 V the module cannot regulate to 5 V.
- The LM2596 module may need a minimum load. Connect a 1 kΩ load resistor
  between OUT+ and OUT− temporarily to stabilise it.

### Firmware build fails

```bash
# Clean and try again
cd firmware/device
cargo clean
cargo build --release
```

If it fails with "error: linker not found": the `espflash` or `esp-idf` component
is missing. Follow the toolchain setup in Stage 6.1.

---

## Reference: complete wiring at a glance

```
  ┌─ Power ──────────────────────────────────────────────────────┐
  │  5× LR44 (+) → LM2596 IN+                                   │
  │  5× LR44 (−) → LM2596 IN−                                   │
  │  LM2596 OUT+ → 5 V breadboard rail (+ DevKit VIN)           │
  │  LM2596 OUT− → GND breadboard rail (+ DevKit GND)           │
  │  C1 100µF: 5 V rail → GND (near motor)                      │
  │  C2 100nF: DevKit 3V3 → GND (close to chip)                 │
  └──────────────────────────────────────────────────────────────┘

  ┌─ Motor driver ───────────────────────────────────────────────┐
  │  GPIO4 → R1 (1kΩ) → Q1 Base                                 │
  │  Q1 Emitter → GND                                           │
  │  Q1 Collector → Motor (−)                                   │
  │  Motor (+) → 5 V rail                                       │
  │  D1 cathode (band) → 5 V rail                               │
  │  D1 anode → Q1 Collector                                    │
  └──────────────────────────────────────────────────────────────┘

  ┌─ LED ────────────────────────────────────────────────────────┐
  │  GPIO5 → R2 (220Ω) → LED Anode (+, long leg)                │
  │  LED Cathode (−, short leg) → GND                           │
  └──────────────────────────────────────────────────────────────┘
```

Detailed schematic with Mermaid diagrams: `firmware/hardware/wiring.md`
Full bill of materials: `firmware/hardware/bom.md`
