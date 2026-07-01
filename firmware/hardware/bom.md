# Bill of Materials – KineticJewel

This is the prototype/breadboard BOM. For a size-optimized production
version with SMD parts and a rechargeable Li-Po battery charged via
pogo-pad contacts, see `bom-production.md`. The USB-C charging base that
mates with those contacts (and can charge two devices at once) is a
separate BOM: `bom-charging-dock.md`. For all three boards in one table,
see `bom-unified.md`.

## All components – confirmed on hand

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

---

## Power options compared

| Option                        | Efficiency | Notes                                       |
|:------------------------------|:----------:|:--------------------------------------------|
| LM2596 + 5× LR44 (7.5V)      | ~85%       | Recommended — all button cells, compact      |
| LM2596 + 9V PP3 block         | ~85%       | Easiest single battery; ~500 mAh            |
| LM2596 + 4× AA (6V)           | ~85%       | Longest runtime; larger physical footprint   |
| L7805 + 5× LR44 (7.5V)        | ~67%       | Simpler but wastes ~33% as heat             |

For wearable use: **LM2596 + 5× LR44** keeps the form factor small and efficient.
