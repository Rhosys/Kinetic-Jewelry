# Bill of Materials – KineticJewel

## You already have

| Part              | Used for                          | Notes                             |
|:------------------|:----------------------------------|:----------------------------------|
| ESP32-C3 DevKit   | Microcontroller + BLE             | Has onboard 5V→3.3V regulator     |
| 2N2222 NPN        | Motor switch transistor           | TO-92 package                     |
| L7805CV           | 5V regulator (alternative path)   | Needs ≥7V input; less efficient   |
| LM2596 module     | 5V regulator (recommended)        | Set trimmer to 5.0V before wiring |
| LR44 cells        | Power source                      | Need 5× in series (7.5V) for L7805; or 4×AA for LM2596 |
| Resistors         | R1 1kΩ (base), R2 220Ω (LED)      | Both common values                |
| Capacitors        | C1 100µF bulk, C2 100nF decouple  | Electrolytic + ceramic            |

## Still need to buy

| Part              | Why                               | Common value / part               |
|:------------------|:----------------------------------|:----------------------------------|
| **Flyback diode** | Protects 2N2222 from motor spike  | **1N4001** (preferred) or 1N4148  |
| Vibration motor   | The output device                 | ERM coin 8mm×3.4mm, 3–5V, ~70mA  |
| LED               | Status indicator                  | Any 3mm, Vf ≈ 2.0V               |
| Battery holder    | Holds cells in series             | 5×LR44 stacked, or AA 4-pack     |

## Power options compared

| Option                        | Efficiency | Size      | Notes                              |
|:------------------------------|:----------:|:---------:|:-----------------------------------|
| LM2596 + 5× LR44 (7.5V)      | ~85%       | Medium    | Recommended — flexible input range |
| LM2596 + 9V PP3               | ~85%       | Small     | Easiest single battery             |
| LM2596 + 4× AA (6V)          | ~85%       | Large     | Longest runtime (~2500 mAh/cell)   |
| L7805 + 5× LR44 (7.5V)       | ~67%       | Medium    | Simpler circuit, wastes 33% as heat|
| L7805 + 9V PP3                | ~56%       | Small     | Gets warm; not great for jewelry   |

For jewelry the LM2596 with a 9V PP3 is the cleanest: one battery, compact, efficient.
