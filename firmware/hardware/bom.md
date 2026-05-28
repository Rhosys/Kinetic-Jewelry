# Bill of Materials – KineticJewel Hardware

| Ref  | Description                          | Value / Part              | Qty | Notes                                         |
|:-----|:-------------------------------------|:--------------------------|:---:|:----------------------------------------------|
| U1   | Microcontroller                      | ESP32-C3 Super Mini       |  1  | Any ESP32-C3 board; WROOM-32 also works       |
| U2   | LDO voltage regulator 3.3 V          | AMS1117-3.3 (SOT-223)     |  1  | MCP1700-3302E (TO-92) is a lower-Iq option    |
| Q1   | NPN BJT transistor                   | 2N2222A or BC337          |  1  | hFE ≥ 100; handles 600 mA+ collector current  |
| D1   | Flyback / clamping diode             | 1N4148                    |  1  | Protects Q1 from motor inductive spike        |
| M1   | ERM coin vibration motor             | 8 mm × 3.4 mm, 3 V–5 V   |  1  | Seeed 107020000 or equivalent ~65–80 mA       |
| LED1 | 3 mm LED                             | Green or white            |  1  | Any colour; Vf ≈ 2.0–2.2 V                    |
| BT1  | Button cell battery holder (×3)      | LR44 / AG13 stacked       |  1  | Or 3× individual holders wired in series      |
| BT–  | LR44 / AG13 alkaline button cell     | 1.5 V, ~150 mAh           |  3  | SR44 silver-oxide gives longer runtime        |
| R1   | Resistor – transistor base           | 1 kΩ, 1/4 W               |  1  | Ensures transistor saturates fully            |
| R2   | Resistor – LED current limit         | 220 Ω, 1/4 W              |  1  | Gives ~6 mA at 3.3 V supply                  |
| C1   | Electrolytic capacitor – bulk        | 100 µF / 10 V             |  1  | Smooths battery internal-resistance droop     |
| C2   | Electrolytic capacitor – LDO output  | 10 µF / 10 V              |  1  | Required by AMS1117 for stability             |
| C3   | Ceramic capacitor – decoupling       | 100 nF / 10 V (0.1 µF)    |  1  | Place as close to ESP32 VCC pin as possible   |

## Optional / upgrades

| Ref  | Description                          | Value / Part              | Notes                                              |
|:-----|:-------------------------------------|:--------------------------|:---------------------------------------------------|
| SW1  | Tactile pushbutton                   | 6 mm × 6 mm               | Manual reset or future pairing button              |
| LED2 | Second LED (status vs vibration)     | Red 3 mm                  | Separate BLE-connected indicator                   |
| U3   | N-channel MOSFET (replaces Q1)       | 2N7002 or DMN2004K        | Lower Vgs(th) – better switch at 3.3 V GPIO level  |
| J1   | Micro-USB or USB-C breakout          | –                         | 5 V power input for bench testing                  |

## Tools needed

- Soldering iron + solder
- Wire (28–30 AWG for signal, 24–26 AWG for power/motor)
- Multimeter (continuity + voltage check)
- USB–serial adapter (for first flash; also used by the ESP32-C3 USB-CDC after)
