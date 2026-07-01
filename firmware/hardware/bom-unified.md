# Bill of Materials – KineticJewel (Unified)

Single-table view of every part across all three boards. Ref designators
are scoped **per board**, not globally — e.g. `U1` on the Production Device
and `U1` on the Charging Dock are unrelated parts on separate physical
boards. Use the **Board** column to disambiguate.

For prose (why a part is there, wiring/charge-path walkthroughs, tradeoffs),
see the per-board files this table is generated from: `bom.md` (Prototype),
`bom-production.md` (Production Device), `bom-charging-dock.md` (Charging
Dock). For a wiring diagram of the Production Device + Charging Dock
together, see `charging-system-diagram.md`.

| Board              | Ref  | Part                          | Value / Spec                             | Qty | Notes                                                                 |
|:-------------------|:-----|:------------------------------|:-------------------------------------------|:---:|:------------------------------------------------------------------------|
| Prototype          | U1   | ESP32-C3 DevKit               | 3.3V MCU + BLE 5.0                        | 1   | Onboard 5V→3.3V regulator; flash via USB-CDC                          |
| Prototype          | VR1  | LM2596 module                 | Adj. buck, 3–40V in → 5V out              | 1   | Set trimmer to 5.0V before connecting anything                        |
| Prototype          | Q1   | 2N2222 NPN                    | TO-92, I_c 600mA, h_FE ≥ 100              | 1   | Motor switch transistor                                                |
| Prototype          | D1   | Diode                         | 1N4001 or similar                         | 1   | Flyback clamp across motor                                             |
| Prototype          | M1   | ERM coin motor                | 3–5V, ~70mA                               | 1   | 8mm × 3.4mm coin type recommended                                     |
| Prototype          | LED1 | LED                           | Any colour, V_f ≈ 2.0V                    | 1   | Status / vibration indicator                                           |
| Prototype          | BT1  | LR44 cells                    | 1.5V each                                 | 5   | In series = 7.5V → into LM2596 input                                  |
| Prototype          | R1   | Resistor                      | 1 kΩ ¼W                                   | 1   | 2N2222 base drive                                                      |
| Prototype          | R2   | Resistor                      | 220 Ω ¼W                                  | 1   | LED current limit                                                      |
| Prototype          | C1   | Electrolytic cap              | 100 µF / 10V                              | 1   | Bulk decoupling on 5V rail, near motor                                 |
| Prototype          | C2   | Ceramic cap                   | 100 nF (0.1µF) / 10V                      | 1   | High-freq decoupling, close to DevKit VCC pin                          |
| Production Device  | U1   | ESP32-C3-MINI-1               | SMD module, integrated flash+antenna      | 1   | Castellated-edge module; replaces DevKit board                          |
| Production Device  | Q1   | MMBT2222A                     | SOT-23, I_c 600mA, h_FE ≥ 100             | 1   | SMD equivalent of 2N2222; motor switch transistor                      |
| Production Device  | D1   | SS14 (or 1N4148WS)            | Schottky, SOD-123                         | 1   | Flyback clamp across motor                                             |
| Production Device  | M1   | ERM coin motor                | 3–5V, ~70mA                               | 1   | Same as prototype — already small                                      |
| Production Device  | LED1 | LED                           | 0805, any colour, V_f ≈ 2.0V              | 1   | Status / vibration indicator                                           |
| Production Device  | R1   | Resistor                      | 1 kΩ, 0603                                | 1   | Motor transistor base drive                                            |
| Production Device  | R2   | Resistor                      | 220 Ω, 0603                               | 1   | LED1 current limit                                                     |
| Production Device  | C1   | MLCC / tantalum cap           | 22 µF, 0805                               | 1   | Bulk decoupling on 3.3V rail, near motor                                |
| Production Device  | C2   | Ceramic cap                   | 100 nF, 0402/0603                         | 1   | High-freq decoupling, close to module VDD pin                          |
| Production Device  | BT1  | Li-Po pouch cell               | 3.7V nominal, single cell, 40–100mAh      | 1   | JST-PH 2-pin; **no built-in protection** (U3 handles that)             |
| Production Device  | U4   | AP2112K-3.3                   | LDO, SOT-23-5, 600mA                      | 1   | Battery (3.0–4.2V) → 3.3V for U1                                       |
| Production Device  | C3   | Ceramic cap                   | 1 µF, 0603                                | 1   | LDO input decoupling                                                    |
| Production Device  | C4   | Ceramic cap                   | 1 µF, 0603                                | 1   | LDO output decoupling                                                   |
| Production Device  | J1   | 2-pin magnetic pogo connector  | Pad/target half                           | 1   | Mates with dock's spring-pin half                                      |
| Production Device  | F1   | PPTC resettable fuse           | 0603, ~750mA hold                         | 1   | Short-circuit protection on exposed pogo pads                          |
| Production Device  | U2   | MCP73831T-2ACI/OT              | Li-Po linear charger, SOT-23-5            | 1   | Charges BT1; 4.2V termination                                           |
| Production Device  | U3   | DW01A + FS8205A                | Protection IC + dual N-MOSFET, SOT-23-6 ×2 | 1 pair | Over-charge/discharge/short/over-current protection                 |
| Production Device  | R3   | Resistor (R_PROG)              | 2 kΩ, 0603                                | 1   | Sets charge current ≈ 500mA                                            |
| Production Device  | LED2 | LED (charge status)            | 0603/0805, any colour                     | 1   | Driven by MCP73831 STAT pin                                            |
| Production Device  | R6   | Resistor                       | 1 kΩ, 0603                                | 1   | LED2 current limit                                                     |
| Production Device  | C5   | Ceramic cap                    | 4.7 µF, 0603                              | 1   | MCP73831 input decoupling                                              |
| Charging Dock       | J1   | USB-C receptacle               | 16-pin SMD (e.g. GCT USB4085 or equiv.)   | 1   | Power-only usage — data pins (D+/D−, SBU) left unconnected             |
| Charging Dock       | R1   | Resistor (CC1 pulldown)        | 5.1 kΩ, 0603                              | 1   | Advertises J1 as a USB-C sink so USB-C chargers/hosts supply 5V         |
| Charging Dock       | R2   | Resistor (CC2 pulldown)        | 5.1 kΩ, 0603                              | 1   | Same as R1 — both CC pins must be pulled down independently            |
| Charging Dock       | F1   | PPTC resettable fuse            | 0603/1206, ~2A hold                       | 1   | Main input protection, sized for two ports combined                    |
| Charging Dock       | C1   | Ceramic / tantalum cap          | 22 µF, 0805                               | 1   | Bulk decoupling on VBUS, feeds both ports                              |
| Charging Dock       | J2   | 2-pin magnetic pogo connector   | Spring-pin half                           | 1   | Port A — mates with a device's J1 pad half                            |
| Charging Dock       | J3   | 2-pin magnetic pogo connector   | Spring-pin half                           | 1   | Port B — mates with a second device's J1 pad half                     |
| Charging Dock       | F2   | PPTC resettable fuse            | 0603, ~750mA hold                         | 1   | Per-port protection for Port A                                         |
| Charging Dock       | F3   | PPTC resettable fuse            | 0603, ~750mA hold                         | 1   | Per-port protection for Port B                                         |
| Charging Dock       | LED1 | LED (power-present, Port A)     | 0805, any colour                          | 1   | Lit when a device is seated on Port A and drawing power                |
| Charging Dock       | LED2 | LED (power-present, Port B)     | 0805, any colour                          | 1   | Same as LED1, for Port B                                               |
| Charging Dock       | R3   | Resistor                        | 1 kΩ, 0603                                | 1   | LED1 current limit                                                     |
| Charging Dock       | R4   | Resistor                        | 1 kΩ, 0603                                | 1   | LED2 current limit                                                     |

## Part count by board

| Board              | Line items | Notes                                                    |
|:-------------------|:----------:|:-----------------------------------------------------------|
| Prototype           | 11         | Breadboard/THT build — see `bom.md`                        |
| Production Device   | 20         | SMD + rechargeable battery + pogo-pad charging interface   |
| Charging Dock       | 13         | Passive USB-C hub, two independent pogo-pin ports          |
