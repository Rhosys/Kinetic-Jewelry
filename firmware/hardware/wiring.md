# KineticJewel – Circuit Design

## Components on hand

| Part                | Notes                                              |
|:--------------------|:---------------------------------------------------|
| ESP32-C3 DevKitM-1  | Has onboard 5V→3.3V regulator; accepts 5V on VIN  |
| 2N2222 NPN          | Motor switch transistor                            |
| L7805CV             | Fixed 5V LDO – **needs ≥7V input**                |
| LM2596 module       | Adjustable buck converter, 3–40V in, 1.5–35V out  |
| LR44 cells          | 1.5V each                                          |
| Resistors, caps     | ✓                                                  |
| Flyback diode       | **You need one** – 1N4001 or 1N4148; without it   |
|                     | the motor's inductive kick will damage the 2N2222  |

---

## Power design

The ESP32-C3 DevKit takes **5V on its VIN pin** and has an onboard AMS1117-3.3
that powers the chip.  The motor also runs from the 5V rail.

### Recommended path: LM2596 module → 5V

The LM2596 module accepts 3–40V in and has a trimmer pot to set the output.
Set the output to **5.0V** (measure with a multimeter while adjusting).

```
  Battery stack ──► [LM2596 IN+]
                         │
                    [LM2596 module]  (turn trimmer until VOUT = 5.0V)
                         │
                    [LM2596 OUT+] ──────────────────────► DevKit VIN
                                  └──────────────────────► motor (+) rail
  Battery (–) ──► [LM2596 IN–/OUT– GND] ──────────────► DevKit GND
```

**Minimum battery stack for LM2596 → 5V:**
The LM2596 needs ~1.5V headroom above its output, so input must be ≥ 6.5V.

| Battery option          | Voltage    | Notes                                     |
|:------------------------|:----------:|:------------------------------------------|
| 5× LR44 in series       | 7.5V       | Compact; 150 mAh per cell                 |
| 4× AA alkaline          | 6V         | More capacity (~2500 mAh), larger          |
| 9V PP3 ("square") block | 9V         | Easiest single battery; ~500 mAh          |

### Alternative path: L7805 → 5V

The L7805CV drops input to a fixed 5V but needs ≥7V in (it has ~2V dropout).
It dissipates the excess as heat — less efficient than the LM2596.

```
  ≥7V source ──► [L7805 IN]──[L7805 OUT]──► DevKit VIN  +  motor (+) rail
  GND ──────────► [L7805 GND]
```

Requires 5× LR44 (7.5V) or a 9V battery.
Add 0.33µF ceramic cap on IN and 0.1µF on OUT (if not already on your DevKit).

---

## Vibration motor driver

The 2N2222 switches the motor's ground leg; the motor positive goes straight to
the 5V rail.  **A flyback diode across the motor is mandatory** — the coil
inside the ERM motor produces a voltage spike when current is cut that will
exceed the 2N2222's collector-emitter breakdown voltage (~40V spike is common
from a small ERM motor).

```
  5V rail ──────────────────────────────── motor (+)
                                               │
                      [diode cathode] ─────────┘  ← flyback diode
                      [diode anode] ──┐
                                      │
                                  motor (–)
                                      │
                                 [Collector]  2N2222
                                      │
  GPIO 4 ──[R1 1kΩ]──[Base]     [Emitter]
                                      │
                                     GND
```

**R1 sizing check (1 kΩ):**
- GPIO high = 3.3V; V_BE ≈ 0.7V → I_base = (3.3 − 0.7) / 1000 = **2.6 mA**
- Motor draws ~70 mA; h_FE for 2N2222 ≥ 100 → needs ≥ 0.7 mA base to saturate
- 2.6 mA >> 0.7 mA ✓  — transistor is fully saturated at motor load

The 2N2222 in TO-92 package: flat face toward you → left=Emitter, middle=Base, right=Collector.

---

## LED

```
  GPIO 5 ──[R2 220Ω]──[LED anode]──[LED cathode]──GND
```

(3.3 − 2.0) / 220 ≈ 6 mA — safe and visible.

---

## Decoupling capacitors

Place these as close to the ESP32-C3 DevKit VIN/GND pins as practical:

| Cap  | Value    | Purpose                                       |
|:-----|:---------|:----------------------------------------------|
| C1   | 100µF    | Bulk cap on 5V rail — absorbs motor-start surge|
| C2   | 100nF    | High-frequency decoupling on DevKit VCC        |

---

## Pin map

| Function    | GPIO | Direction | Notes                    |
|:------------|:----:|:---------:|:-------------------------|
| Motor drive | 4    | Output    | HIGH = 2N2222 on = motor |
| LED         | 5    | Output    | HIGH = LED on            |

---

## Wiring summary

```
  [Battery]──[LM2596]─────5V──────────────────────► DevKit VIN
                      └───5V──────────────────────► motor (+)
                           │
                          GND ──────────────────── DevKit GND

  DevKit GPIO4 ──[1kΩ]──[2N2222 Base]
               [2N2222 Emitter]──GND
               [2N2222 Collector]──motor(–)
               [flyback diode: anode→collector, cathode→motor(+)/5V]

  DevKit GPIO5 ──[220Ω]──[LED+]──[LED–]──GND

  [C1 100µF] between 5V and GND   (near LM2596 output / motor)
  [C2 100nF] between DevKit VCC and GND
```

---

## Still needed

- **Flyback diode** (1N4001 or 1N4148) — the single missing component.
  Both are cheap and widely available.  The 1N4001 is a better choice here
  (it's rated for higher surge current from the motor inrush).
