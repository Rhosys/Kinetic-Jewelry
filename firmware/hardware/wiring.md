# KineticJewel – Circuit Design

## Target module: ESP32-C3 Super Mini (or DevKitM-1 for development)

The ESP32-C3 runs from **3.0 V – 3.6 V** on its VDD pins.  GPIO outputs are
3.3 V logic.  The module is RISC-V based, which makes the Rust toolchain simpler
(no custom Xtensa LLVM needed).

---

## Power supply

### Option A – DevKit with onboard 5 V → 3.3 V regulator (development)

If you have a 5 V source (USB, or your DC converter fed from 6–9 V):

```
  5 V source ──► [DevKit 5V / VIN pin]
                         │
                   [onboard AMS1117-3.3]
                         │
                       3.3 V ──► ESP32-C3 VCC
                       5 V  ──► motor rail (more force)
                       GND  ──► GND
```

### Option B – LR44 batteries standalone (wearable)

Three LR44 cells in series = 4.5 V nominal (drops to ~3.6 V at end of life).
An AMS1117-3.3 LDO drops this to a clean 3.3 V for the ESP32.
The motor is driven from the raw 4.5 V rail for full vibration force.

```
  LR44 × 3  (+4.5 V)
     │
     ├──[C1 100µF]──GND     ← absorbs current spikes from motor switching
     │
     ├──[AMS1117-3.3 IN]
     │         │
     │   [AMS1117-3.3 OUT]──[C2 10µF]──GND    ← LDO output cap (required)
     │         │             [C3 100nF]──GND   ← decoupling cap (near ESP32)
     │         │
     │       3.3 V ──────────────────► ESP32-C3 VCC + LED circuit
     │
     └─────────────────────────────────────────► motor (+) terminal  [4.5 V]
     GND ────────────────────────────────────► GND
```

> **2× LR44 shortcut (no regulator):**  Two cells = 3.0 V, which is within
> spec for ESP32-C3 (min 2.3 V).  Works without the AMS1117 for a simpler
> circuit, at the cost of ~15% less battery life and reduced motor force.

---

## Vibration motor driver

ERM coin motors draw 65–80 mA – far more than any GPIO can supply.
An NPN transistor (2N2222A or BC337) switches the motor; a 1N4148 diode clamps
the inductive spike when the motor is turned off.

```
  Motor (+) ───────────────────────────── 4.5 V (or 5 V rail)
  Motor (–) ──────────────── [Collector]
                                   │
  [1N4148 anode] ──── [Collector]  │   ← flyback diode
  [1N4148 cathode] ─── motor (+)   │
                                   │
                             [2N2222A / BC337]
                                   │
                           [Base] ──── [R1 1 kΩ] ──── GPIO 4
                                   │
                             [Emitter]
                                   │
                                  GND
```

**Why 1 kΩ for R1?**
GPIO output = 3.3 V, V_BE ≈ 0.7 V → I_base ≈ 2.6 mA.
Motor draws ~70 mA; transistor h_FE ≥ 100 → needs ≥ 0.7 mA base.
2.6 mA drives the transistor well into saturation at full motor current.

---

## LED

```
  GPIO 5 ──── [R2 220 Ω] ──── [LED anode] ──── [LED cathode] ──── GND
```

Forward current at 3.3 V: (3.3 – 2.0) / 220 ≈ **6 mA** — safe, visible.
Adjust R2 to 330 Ω for dimmer / lower power, or 100 Ω for brighter.

---

## Complete pin map

| Function    | GPIO (ESP32-C3) | Direction | Notes                       |
|:------------|:---------------:|:---------:|:----------------------------|
| Motor drive | 4               | Output    | HIGH = transistor on        |
| LED         | 5               | Output    | HIGH = LED on               |
| USB TX      | 21              | Output    | Serial monitor (115200)     |
| USB RX      | 20              | Input     | (reserved for flashing)     |

> **Other ESP32 boards:** change `PIN_MOTOR` and `PIN_LED` in
> `firmware/src/config.rs`.

---

## Full topology summary

```
  ┌──────────────────────────────────────────────────────────────┐
  │  Power                                                        │
  │   LR44×3 (+)  →  C1 100µF  →  AMS1117-3.3  →  3.3V → ESP32 │
  │                            └────────────────────────→ motor+ │
  │   LR44×3 (–)  →  GND                                         │
  └──────────────────────────────────────────────────────────────┘
  ┌──────────────────────────────────────────────────────────────┐
  │  Motor                                                        │
  │   GPIO4 → R1(1kΩ) → NPN base                                 │
  │                      NPN collector → motor(–)                │
  │                      NPN emitter   → GND                     │
  │   motor(+) → 4.5V                                            │
  │   1N4148: anode→collector, cathode→motor(+)                  │
  └──────────────────────────────────────────────────────────────┘
  ┌──────────────────────────────────────────────────────────────┐
  │  LED                                                          │
  │   GPIO5 → R2(220Ω) → LED(+) → LED(–) → GND                  │
  └──────────────────────────────────────────────────────────────┘
```
