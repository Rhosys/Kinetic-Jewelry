# Bill of Materials – KineticJewel

Single source of truth for every part across the three physical boards:
**Prototype** (breadboard), **Production Device** (SMD, rechargeable,
pogo-pad charging), and **Charging Dock** (USB-C, charges up to two devices
at once). Ref designators are scoped **per board**, not globally — e.g.
`U1` on the Production Device and `U1` on the Charging Dock are unrelated
parts on separate physical boards.

For a wiring diagram of the Production Device + Charging Dock together, see
`charging-system-diagram.md`.

---

## Prototype (breadboard)

All components confirmed on hand.

| Ref  | Part                  | Value / Spec                    | Qty | Notes                                          |
|:-----|:----------------------|:--------------------------------|:---:|:-----------------------------------------------|
| U1   | ESP32-C3 DevKit       | 3.3V MCU + BLE 5.0              |  1  | Onboard 5V→3.3V regulator; flash via USB-CDC   |
| VR1  | LM2596 module         | Adj. buck, 3–40V in → 5V out   |  1  | Set trimmer to 5.0V before connecting anything |
| Q1   | 2N2222 NPN            | TO-92, I_c 600mA, h_FE ≥ 100  |  1  | Motor switch transistor                        |
| D1   | Diode                 | 1N4001 or similar              |  1  | Flyback clamp across motor                     |
| M1   | ERM coin motor        | 3–5V, ~70mA                    |  1  | 8mm × 3.4mm coin type recommended              |
| LED1 | LED                   | Any colour, V_f ≈ 2.0V         |  1  | Status / vibration indicator                   |
| BT1  | LR44 cells            | 1.5V each                      |  5  | In series = 7.5V → into LM2596 input          |
| R1   | Resistor              | 1 kΩ  ¼W                       |  1  | 2N2222 base drive                             |
| R2   | Resistor              | 220 Ω  ¼W                      |  1  | LED current limit                              |
| C1   | Electrolytic cap      | 100 µF / 10V                   |  1  | Bulk decoupling on 5V rail, near motor         |
| C2   | Ceramic cap           | 100 nF (0.1µF) / 10V           |  1  | High-freq decoupling, close to DevKit VCC pin  |

**Nothing missing — build when ready.**

### Power options compared

| Option                        | Efficiency | Notes                                       |
|:------------------------------|:----------:|:--------------------------------------------|
| LM2596 + 5× LR44 (7.5V)      | ~85%       | Recommended — all button cells, compact      |
| LM2596 + 9V PP3 block         | ~85%       | Easiest single battery; ~500 mAh            |
| LM2596 + 4× AA (6V)           | ~85%       | Longest runtime; larger physical footprint   |
| L7805 + 5× LR44 (7.5V)        | ~67%       | Simpler but wastes ~33% as heat             |

For wearable use: **LM2596 + 5× LR44** keeps the form factor small and efficient.

---

## Production Device (SMD, rechargeable, pogo-pad charging)

Same electrical function as the Prototype (ESP32-C3 + BLE, motor vibration
driver, status LED), re-specced with SMD parts for a small wearable
footprint, plus a rechargeable Li-Po battery charged via 2-pin pogo-pad
contacts.

### Core (MCU, motor driver, LED)

| Ref  | Part                       | Value / Spec                         | Qty | Notes                                                    |
|:-----|:---------------------------|:--------------------------------------|:---:|:----------------------------------------------------------|
| U1   | ESP32-C3-MINI-1            | SMD module, integrated flash+antenna | 1   | Castellated-edge module; replaces DevKit board            |
| Q1   | MMBT2222A                  | SOT-23, I_c 600mA, h_FE ≥ 100        | 1   | SMD equivalent of 2N2222; motor switch transistor         |
| D1   | SS14 (or 1N4148WS)          | Schottky, SOD-123                    | 1   | Flyback clamp across motor                                |
| M1   | ERM coin motor              | 3–5V, ~70mA                          | 1   | Same as prototype — already small                         |
| LED1 | LED                         | 0805, any colour, V_f ≈ 2.0V         | 1   | Status / vibration indicator                               |
| R1   | Resistor                   | 1 kΩ, 0603                           | 1   | Motor transistor base drive                                |
| R2   | Resistor                   | 220 Ω, 0603                          | 1   | LED1 current limit                                          |
| C1   | MLCC / tantalum cap         | 22 µF, 0805                          | 1   | Bulk decoupling on 3.3V rail, near motor (replaces 100µF electrolytic) |
| C2   | Ceramic cap                 | 100 nF (0.1 µF), 0402/0603            | 1   | High-freq decoupling, close to module VDD pin              |

### Power: battery + regulation

| Ref  | Part                       | Value / Spec                         | Qty | Notes                                                    |
|:-----|:---------------------------|:--------------------------------------|:---:|:----------------------------------------------------------|
| BT1  | Li-Po pouch cell            | 3.7V nominal, single cell, 40–100mAh | 1   | Size to fit enclosure; JST-PH 2-pin connector; **no built-in protection PCB** (protection is provided by U3 below — avoid stacking two protection circuits) |
| U4   | AP2112K-3.3                | LDO, SOT-23-5, 600mA, ~250mV dropout | 1   | Regulates battery (3.0–4.2V) down to 3.3V for U1; replaces LM2596 buck module |
| C3   | Ceramic cap                 | 1 µF, 0603                           | 1   | LDO input decoupling                                       |
| C4   | Ceramic cap                 | 1 µF, 0603                           | 1   | LDO output decoupling                                       |

LDO vs. buck tradeoff: ~70–80% efficiency (vs. ~85% for the LM2596 buck) in
exchange for a ~10x smaller footprint and 2 fewer parts — the right tradeoff
for a wearable where board area matters more than a few extra mA draw.

### Pogo-pad charging interface

The USB-C receptacle and its CC1/CC2 resistor network live on the Charging
Dock (below), not the device. The device only exposes two flat contact pads
that the dock's spring-loaded pins land on when the piece is set down on it
(same approach as most smartwatches/fitness bands). Keeping the connector
off the device removes the biggest single footprint item from a
USB-C-on-device design and gives up a sealed, port-free case.

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

### Full charge path

```
Dock spring pins (see Charging Dock section below)
  → J1 (device pogo pad, V+ / GND)
  → F1 (PPTC fuse)
  → U2 (MCP73831) charges BT1 through U3 (DW01A/FS8205A protection)
  → BT1 (3.7V Li-Po)
  → U4 (AP2112K-3.3 LDO) → 3.3V rail → U1 (ESP32-C3-MINI-1), LED1, motor driver
```

### Size/part-count comparison vs. Prototype

| Aspect              | Prototype                          | Production Device                              |
|:---------------------|:-----------------------------------|:-------------------------------------------------|
| MCU                  | ESP32-C3 DevKit (full board)      | ESP32-C3-MINI-1 (bare SMD module)               |
| Power source          | 5× LR44 disposable cells          | Single rechargeable Li-Po cell                  |
| Regulation            | LM2596 buck module                | AP2112K-3.3 LDO (SOT-23-5)                      |
| Charging              | None (disposable)                 | Pogo-pad contacts, MCP73831 + DW01A/FS8205A protection (USB-C lives on the separate dock) |
| Passive packages      | ¼W THT resistors, THT/electrolytic caps | 0603/0805 SMD resistors and caps           |
| Transistor/diode      | TO-92 / THT diode                 | SOT-23 / SOD-123 SMD                            |

**Nothing here is exotic — every part above is a standard, widely-stocked
SMD component.** Build once the core protocol is stable.

---

## Charging Dock (USB-C, dual port)

A separate, passive charging base: takes USB-C power in and delivers it to
one or two Production Devices via pogo-pin contacts. It has no MCU and does
no charge management itself — each device's own MCP73831 + DW01A/FS8205A
circuit (above) handles its own charge current and battery protection. The
dock's only job is to accept power from either USB-A→C or USB-C→C cables
and fan it out to two contact ports.

### USB-C power input

| Ref  | Part                          | Value / Spec                            | Qty | Notes                                                                 |
|:-----|:------------------------------|:------------------------------------------|:---:|:------------------------------------------------------------------------|
| J1   | USB-C receptacle               | 16-pin SMD (e.g. GCT USB4085 or equiv.)   | 1   | Power-only usage — data pins (D+/D−, SBU) left unconnected            |
| R1   | Resistor (CC1 pulldown)        | 5.1 kΩ, 0603                              | 1   | Advertises J1 as a USB-C sink (UFP) so USB-C chargers/hosts supply default 5V |
| R2   | Resistor (CC2 pulldown)        | 5.1 kΩ, 0603                              | 1   | Same as R1 — both CC pins must be pulled down independently            |
| F1   | PPTC resettable fuse            | 0603/1206, ~2A hold current               | 1   | Main input protection, sized for two ports drawing up to ~1A combined plus margin |
| C1   | Ceramic / tantalum cap          | 22 µF, 0805                               | 1   | Bulk decoupling on VBUS, feeds both ports                              |

#### Why both USB-A→C and USB-C→C cables work

- **USB-A→C cable**: the host side has no CC pins at all, so it never
  negotiates — it just presents default 5V on VBUS the moment the cable is
  plugged in.
- **USB-C→C cable**: USB-C sources won't turn on VBUS until they detect a
  valid sink pulling CC1/CC2 low. **R1 and R2 are what make this path
  work** — without them, USB-C-only chargers/PD bricks see an open CC line
  and withhold power.

### Dual charging ports

| Ref  | Part                          | Value / Spec                            | Qty | Notes                                                                 |
|:-----|:------------------------------|:------------------------------------------|:---:|:------------------------------------------------------------------------|
| J2   | 2-pin magnetic pogo connector  | Spring-pin half                           | 1   | Port A — mates with a device's J1 pad half                            |
| J3   | 2-pin magnetic pogo connector  | Spring-pin half                           | 1   | Port B — mates with a second device's J1 pad half                     |
| F2   | PPTC resettable fuse            | 0603, ~750mA hold current                 | 1   | Per-port protection for Port A (independent of F1)                    |
| F3   | PPTC resettable fuse            | 0603, ~750mA hold current                 | 1   | Per-port protection for Port B                                        |
| LED1 | LED (power-present, Port A)    | 0805, any colour                          | 1   | Lit whenever a device is seated on Port A and drawing power — indicates contact/power presence only, not charge completion (that's on-device) |
| LED2 | LED (power-present, Port B)    | 0805, any colour                          | 1   | Same as LED1, for Port B                                              |
| R3   | Resistor                       | 1 kΩ, 0603                                | 1   | LED1 current limit                                                     |
| R4   | Resistor                       | 1 kΩ, 0603                                | 1   | LED2 current limit                                                     |

Both ports are wired in parallel off the same VBUS/GND rail (each behind its
own PPTC fuse) — there's no switching or sequencing logic, so both devices
can charge simultaneously without any coordination between them.

### Power budget for simultaneous charging

Each device charges at whatever its own R_PROG sets (default ~500mA per the
Production Device section above). Charging two devices at once means the
dock needs to source **up to ~1A total** at 5V (10W) with headroom above
that.

- A basic USB 2.0-only 5V/0.5A source will only comfortably charge **one**
  device at full rate — with two devices connected, both will draw from the
  same limited supply and charge slower (or one port may brown out
  depending on source behavior).
- **A USB-C source rated 5V/2A (10W) or better is recommended** for both
  ports to charge at full programmed current at the same time. Most modern
  USB-C wall chargers (even non-PD ones) meet this by default via their CC
  pull-up resistor value — no special negotiation is required on the dock
  side since it isn't doing active current requests.

If reliable full-rate dual charging from lower-power sources matters, the
fix is on the device side (lower R_PROG, e.g. 4kΩ ≈ 250mA per device, so two
devices together stay under 500mA) rather than adding logic to the dock.

### Full charge path

```
USB-C plug (A→C or C→C cable)
  → J1 (VBUS, GND, CC1, CC2)
      CC1 ── R1 (5.1k) ── GND
      CC2 ── R2 (5.1k) ── GND
  → F1 (main PPTC fuse) → shared VBUS rail
      ├── F2 → J2 (Port A pogo pins) → device A's own charge circuit
      └── F3 → J3 (Port B pogo pins) → device B's own charge circuit
```

**Nothing here is exotic — every part above is a standard, widely-stocked
SMD component.** No firmware, no MCU — this is a purely passive power hub.

---

## Part count by board

| Board              | Line items | Notes                                                    |
|:-------------------|:----------:|:-----------------------------------------------------------|
| Prototype           | 11         | Breadboard/THT build                                       |
| Production Device   | 20         | SMD + rechargeable battery + pogo-pad charging interface   |
| Charging Dock       | 13         | Passive USB-C hub, two independent pogo-pin ports          |
