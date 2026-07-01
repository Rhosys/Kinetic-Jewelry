# Bill of Materials – KineticJewel (Production / Miniaturized)

Same electrical function as `bom.md` (ESP32-C3 + BLE, motor vibration driver,
status LED), re-specced with SMD parts for a small wearable footprint, plus a
rechargeable Li-Po battery charged via 2-pin pogo-pad contacts. See `bom.md`
for the prototype/breadboard version this replaces, and
`bom-charging-dock.md` for the separate USB-C charging base that mates with
these contacts (and can charge two devices at once). A full-system Mermaid
diagram tying this file and the dock together is in
`charging-system-diagram.md`. All three boards in one table: `bom-unified.md`.

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

## Pogo-pad charging interface

The USB-C receptacle and its CC1/CC2 resistor network live on the **charging
dock**, not the device — see `bom-charging-dock.md`. The device only exposes
two flat contact pads that the dock's spring-loaded pins land on when the
piece is set down on it (same approach as most smartwatches/fitness bands).
Keeping the connector off the device removes the biggest single footprint
item from the previous USB-C-on-device version and gives up a sealed,
port-free case.

| Ref  | Part                          | Value / Spec                          | Qty | Notes                                                                 |
|:-----|:------------------------------|:----------------------------------------|:---:|:------------------------------------------------------------------------|
| J1   | 2-pin magnetic pogo connector  | Pad/target half (flat contacts + magnet) | 1   | Mates with dock's spring-pin half; magnet gives self-alignment + retention |
| F1   | PPTC resettable fuse            | 0603, ~750mA hold current               | 1   | Short-circuit protection on the exposed contacts (keys, moisture, metal jewelry findings can bridge them) |
| U2   | MCP73831T-2ACI/OT              | Li-Po linear charge mgmt IC, SOT-23-5   | 1   | Charges BT1 from the pogo pad input; regulates to 4.2V termination     |
| U3   | DW01A + FS8205A                | Battery protection IC + dual N-MOSFET, SOT-23-6 ×2 | 1 pair | Over-charge / over-discharge / short-circuit / over-current protection for BT1 |
| R3   | Resistor (R_PROG)              | 2 kΩ, 0603                              | 1   | Sets MCP73831 charge current: I_reg ≈ 1000V / R_PROG ≈ 500 mA (lower to 4kΩ ≈ 250mA if BT1 capacity is small) |
| LED2 | LED (charge status)            | 0603/0805, any colour                   | 1   | Driven by MCP73831 STAT pin — lit while charging, off when charged/no input |
| R6   | Resistor                       | 1 kΩ, 0603                              | 1   | LED2 current limit on STAT output                                       |
| C5   | Ceramic cap                    | 4.7 µF, 0603                            | 1   | MCP73831 input decoupling, close to the pogo-pad power input           |

Charge management (U2/U3/R3) stays on the device rather than the dock so
each piece protects and terminates its own battery correctly regardless of
which dock slot (or which dock) it's charged from.

## Full charge path

```
Dock spring pins (see bom-charging-dock.md)
  → J1 (device pogo pad, V+ / GND)
  → F1 (PPTC fuse)
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
| Charging              | None (disposable)                 | Pogo-pad contacts, MCP73831 + DW01A/FS8205A protection (USB-C lives on the separate dock) |
| Passive packages      | ¼W THT resistors, THT/electrolytic caps | 0603/0805 SMD resistors and caps           |
| Transistor/diode      | TO-92 / THT diode                 | SOT-23 / SOD-123 SMD                            |

**Nothing here is exotic — every part above is a standard, widely-stocked
SMD component.** Build once the core protocol is stable (same gating note as
the prototype BOM).
