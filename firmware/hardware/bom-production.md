# Bill of Materials – KineticJewel (Production / Miniaturized)

Same electrical function as `bom.md` (ESP32-C3 + BLE, motor vibration driver,
status LED), re-specced with SMD parts for a small wearable footprint, plus a
rechargeable Li-Po battery and USB-C charging front end. See `bom.md` for the
prototype/breadboard version this replaces.

## Core (MCU, motor driver, LED)

| Ref  | Part                       | Value / Spec                         | Qty | Notes                                                    |
|:-----|:---------------------------|:--------------------------------------|:---:|:----------------------------------------------------------|
| U1   | ESP32-C3-MINI-1            | SMD module, integrated flash+antenna | 1   | Castellated-edge module; replaces DevKit board            |
| Q1   | MMBT2222A                  | SOT-23, I_c 600mA, h_FE ≥ 100        | 1   | SMD equivalent of 2N2222; motor switch transistor         |
| D1   | SS14 (or 1N4148WS)          | Schottky, SOD-123                    | 1   | Flyback clamp across motor                                |
| M1   | ERM coin motor              | 3–5V, ~70mA                          | 1   | Same as prototype — already small                         |
| LED1 | LED                         | 0805, any colour, V_f ≈ 2.0V         | 1   | Status / vibration indicator                               |
| R1   | Resistor                   | 1 kΩ, 0603                           | 1   | 2N2222/MMBT2222A base drive                                |
| R2   | Resistor                   | 220 Ω, 0603                          | 1   | LED current limit                                          |
| C1   | MLCC / tantalum cap         | 22 µF, 0805                          | 1   | Bulk decoupling on 3.3V rail, near motor (replaces 100µF electrolytic) |
| C2   | Ceramic cap                 | 100 nF (0.1 µF), 0402/0603            | 1   | High-freq decoupling, close to module VDD pin              |

## Power: battery + regulation

| Ref  | Part                       | Value / Spec                         | Qty | Notes                                                    |
|:-----|:---------------------------|:--------------------------------------|:---:|:----------------------------------------------------------|
| BT1  | Li-Po pouch cell            | 3.7V nominal, single cell, 40–100mAh | 1   | Size to fit enclosure; JST-PH 2-pin connector; **no built-in protection PCB** (protection is provided by U3 below — avoid stacking two protection circuits) |
| U4   | AP2112K-3.3                | LDO, SOT-23-5, 600mA, ~250mV dropout | 1   | Regulates battery (3.0–4.2V) down to 3.3V for U1; replaces LM2596 buck module |
| C3   | Ceramic cap                 | 1 µF, 0603                           | 1   | LDO input decoupling                                       |
| C4   | Ceramic cap                 | 1 µF, 0603                           | 1   | LDO output decoupling                                       |

LDO vs. buck tradeoff: ~70–80% efficiency (vs. ~85% for the LM2596 buck) in
exchange for a ~10x smaller footprint and 2 fewer parts — the right tradeoff
for a wearable where board area matters more than a few extra mA draw.

## USB-C charging front end

| Ref  | Part                          | Value / Spec                          | Qty | Notes                                                                 |
|:-----|:------------------------------|:----------------------------------------|:---:|:------------------------------------------------------------------------|
| J1   | USB-C receptacle               | 16-pin SMD (e.g. GCT USB4085 or equiv.) | 1   | Power-only usage — data pins (D+/D−, SBU) left unconnected            |
| U2   | MCP73831T-2ACI/OT              | Li-Po linear charge mgmt IC, SOT-23-5   | 1   | Charges BT1 from VBUS; regulates to 4.2V termination                  |
| U3   | DW01A + FS8205A                | Battery protection IC + dual N-MOSFET, SOT-23-6 ×2 | 1 pair | Over-charge / over-discharge / short-circuit / over-current protection for BT1 |
| R3   | Resistor (R_PROG)              | 2 kΩ, 0603                              | 1   | Sets MCP73831 charge current: I_reg ≈ 1000V / R_PROG ≈ 500 mA (lower to 4kΩ ≈ 250mA if BT1 capacity is small) |
| R4   | Resistor (CC1 pulldown)        | 5.1 kΩ, 0603                            | 1   | Advertises J1 as a USB-C sink (UFP) so USB-C chargers/hosts supply default 5V |
| R5   | Resistor (CC2 pulldown)        | 5.1 kΩ, 0603                            | 1   | Same as R4 — both CC pins must be pulled down independently            |
| LED2 | LED (charge status)            | 0603/0805, any colour                   | 1   | Driven by MCP73831 STAT pin — lit while charging, off when charged/no input |
| R6   | Resistor                       | 1 kΩ, 0603                              | 1   | LED2 current limit on STAT output                                       |
| C5   | Ceramic cap                    | 4.7 µF, 0603                            | 1   | MCU73831 input decoupling near VBUS pin                                 |

### Why both USB-A→C and USB-C→C cables work

- **USB-A→C cable**: the host side has no CC pins at all, so it never
  negotiates — it just presents default 5V on VBUS the moment the cable is
  plugged in. No special handling needed; this already works with a bare
  receptacle.
- **USB-C→C cable (USB-C charger/host on the other end)**: USB-C sources
  will not source power at all unless they detect a valid sink pulling
  CC1/CC2 low. **R4 and R5 (5.1kΩ to GND) are what make this path work** —
  without them, USB-C-only chargers/PD bricks would see an open CC line and
  refuse to output VBUS. With the pulldowns in place, the device is
  correctly detected as a default-current (5V @ up to 3A advertised) USB-C
  sink by any compliant USB-C source.

Together, J1 + R4 + R5 is what lets one receptacle accept power from either
cable type — no mode-detection logic needed, it's purely the resistor
network.

## Full charge path

```
USB-C plug (A→C or C→C cable)
  → J1 (VBUS, GND, CC1, CC2)
      CC1 ── R4 (5.1k) ── GND
      CC2 ── R5 (5.1k) ── GND
  → U2 (MCP73831) charges BT1 through U3 (DW01A/FS8205A protection)
  → BT1 (3.7V Li-Po)
  → U4 (AP2112K-3.3 LDO) → 3.3V rail → U1 (ESP32-C3-MINI-1), LED1, motor driver
```

## Size/part-count comparison vs. prototype BOM

| Aspect              | Prototype (`bom.md`)              | Production (this file)                         |
|:---------------------|:-----------------------------------|:-------------------------------------------------|
| MCU                  | ESP32-C3 DevKit (full board)      | ESP32-C3-MINI-1 (bare SMD module)               |
| Power source          | 5× LR44 disposable cells          | Single rechargeable Li-Po cell                  |
| Regulation            | LM2596 buck module                | AP2112K-3.3 LDO (SOT-23-5)                      |
| Charging              | None (disposable)                 | USB-C in, MCP73831 + DW01A/FS8205A protection   |
| Passive packages      | ¼W THT resistors, THT/electrolytic caps | 0603/0805 SMD resistors and caps           |
| Transistor/diode      | TO-92 / THT diode                 | SOT-23 / SOD-123 SMD                            |

**Nothing here is exotic — every part above is a standard, widely-stocked
SMD component.** Build once the core protocol is stable (same gating note as
the prototype BOM).
