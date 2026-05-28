# Bill of Materials – KineticJewel

## Core components (buy these)

| Ref  | Part                         | Spec                              | Qty | Notes                                        |
|:-----|:-----------------------------|:----------------------------------|:---:|:---------------------------------------------|
| U1   | ESP32-C3 Super Mini          | 3.3 V, BLE 5.0, RISC-V           |  1  | DevKitM-1 for dev; Super Mini for final form |
| U2   | LDO regulator                | AMS1117-3.3  (SOT-223)            |  1  | Skip if using a DevKit with onboard reg      |
| Q1   | NPN transistor               | 2N2222A  or  BC337                |  1  | Motor switch; h_FE ≥ 100, I_c ≥ 600 mA      |
| D1   | Signal diode                 | 1N4148                            |  1  | Flyback clamp across motor                   |
| M1   | ERM coin vibration motor     | 8 mm × 3.4 mm, 3–5 V, ~70 mA     |  1  | Seeed 107020000 or equivalent                |
| LED1 | 3 mm LED                     | Any colour, V_f ≈ 2.0 V           |  1  |                                              |

## From your existing parts bin

| Ref  | Part             | Value         | Qty | Purpose                                   |
|:-----|:-----------------|:--------------|:---:|:------------------------------------------|
| BT1  | LR44 / AG13      | 1.5 V each    |  3  | 4.5 V stack (or 2× for no-regulator build)|
| —    | Battery holder   | 3× LR44       |  1  | Stacked or individual wired in series     |
| R1   | Resistor         | 1 kΩ  ¼ W     |  1  | Transistor base drive                     |
| R2   | Resistor         | 220 Ω  ¼ W    |  1  | LED current limiter                       |
| C1   | Electrolytic cap | 100 µF / 10 V |  1  | Bulk supply decoupling (near battery +)   |
| C2   | Electrolytic cap | 10 µF / 10 V  |  1  | LDO output stabilisation                  |
| C3   | Ceramic cap      | 100 nF / 10 V |  1  | ESP32 VCC decoupling (place ≤5 mm away)   |

## Tooling

- Soldering iron + solder
- Multimeter (continuity + voltage)
- USB–serial adapter (first flash only; ESP32-C3 has USB-CDC built in after)
- `espflash` CLI: `cargo install espflash`

## Power budget (worst case)

| Load                    | Current  |
|:------------------------|:--------:|
| ESP32-C3 BLE active     |  ~25 mA  |
| ESP32-C3 BLE TX peak    |  ~80 mA  |
| ERM motor               |  ~70 mA  |
| LED (on with motor)     |   ~6 mA  |
| AMS1117 quiescent       |   ~5 mA  |
| **Peak total**          | **~161 mA** |

Three LR44 cells are rated ~150 mAh but internal resistance limits burst
current; assume ~100 mAh effective.  The motor and BLE TX are only active in
short bursts, so real-world runtime is many hours of standby between vibrations.
