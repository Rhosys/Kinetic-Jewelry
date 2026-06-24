# KineticJewel – Circuit Design

## Components

| Part              | On hand | Notes                                               |
|:------------------|:-------:|:----------------------------------------------------|
| ESP32-C3 DevKit   | ✓       | Accepts 5V on VIN; has onboard 3.3V regulator       |
| 2N2222 NPN        | ✓       | TO-92: flat face toward you → E left, B middle, C right |
| LM2596 module     | ✓       | Adjust trimmer to 5.0V before connecting            |
| L7805CV           | ✓       | Alternative 5V path if you prefer (needs ≥7V in)   |
| Diodes            | ✓       | One used as flyback clamp across motor              |
| LEDs              | ✓       | One used as status indicator                        |
| Resistors         | ✓       | 1 kΩ (base), 220 Ω (LED)                           |
| Capacitors        | ✓       | 100 µF electrolytic, 100 nF ceramic                 |
| LR44 cells        | ✓       | Use 5× in series = 7.5V                            |
| ERM motor         | ✓       |                                                     |

---

## Schematic (Mermaid)

### Circuit connection diagram

Power rails are circles; components are boxes. Edge labels name the terminal /
net. The motor and flyback diode share the collector node (the diode sits in
parallel across the motor).

```mermaid
flowchart TB
    BAT["Battery<br/>5x LR44 = 7.5V"] -->|"+"| LM["LM2596 buck<br/>(trim to 5.0V)"]
    BAT -->|"-"| GND(("GND"))
    LM -->|"OUT 5V"| RAIL(("5V rail"))
    LM -->|"OUT GND"| GND

    RAIL --- C1["C1 100uF"]
    C1 --- GND
    RAIL -->|"VIN"| ESP["ESP32-C3<br/>DevKit"]
    ESP --- C2["C2 100nF"]
    C2 --- GND

    ESP -->|"GPIO4"| R1["R1 1k ohm"]
    R1 -->|"base"| Q1["Q1 2N2222"]
    Q1 -->|"emitter"| GND
    RAIL -->|"motor +"| M1["ERM motor"]
    M1 -->|"motor -"| COL(("motor- /<br/>collector"))
    COL -->|"collector"| Q1
    RAIL -->|"cathode (band)"| D1["D1 flyback diode"]
    D1 -->|"anode"| COL

    ESP -->|"GPIO5"| R2["R2 220 ohm"]
    R2 -->|"anode"| LED["LED"]
    LED -->|"cathode"| GND

    classDef net fill:#fffbe6,stroke:#b59f00,stroke-width:2px;
    class RAIL,GND,COL net;
```

### Signal flow (phone → outputs)

```mermaid
flowchart LR
    PHONE["Phone app<br/>(BLE central)"] <-->|"BLE GATT"| BLE

    subgraph FW["ESP32-C3 firmware"]
        direction TB
        BLE["ble.rs<br/>GATT server"]
        PROTO["protocol crate<br/>parse()"]
        VIB["vibration.rs<br/>step queue + thread"]
        BLE -->|"raw bytes"| PROTO
        PROTO -->|"VibBlock list"| VIB
    end

    VIB -->|"GPIO4"| MD["2N2222 +<br/>ERM motor"]
    VIB -->|"GPIO5"| LEDO["LED"]
```

### BLE interaction sequence

```mermaid
sequenceDiagram
    participant P as Phone (central)
    participant E as ESP32 (peripheral)
    participant M as Motor / LED
    P->>E: Connect
    P->>E: Read firmware char 7ddac4ce-...324001
    E-->>P: 0x01 (protocol version)
    P->>E: Write command char 7ddac4ce-...324002<br/>[ver, cmd, repeat, blocks...]
    E->>E: parse() -> Vibrate { blocks, repeat }
    loop each block
        E->>M: GPIO high/low for block duration
    end
    Note over P,E: phone may hold the connection<br/>for a response window
    P->>E: Disconnect
    Note over E: NimBLE auto-restarts advertising
```

---

## Power supply

Set the LM2596 trimmer to **5.0V** before wiring anything else — measure with a
multimeter between OUT+ and OUT– while the input is connected.

```
  5× LR44 (+)──────────────► LM2596 IN+
                                  │
                             [LM2596 module]   ← trimmer set to 5.0V
                                  │
                             LM2596 OUT+ ──────────────► DevKit VIN  (5V)
                                         └─────────────► motor (+) rail (5V)
  5× LR44 (–)──────────────► LM2596 IN– / OUT– ────────► DevKit GND
```

---

## Motor driver

The 2N2222 switches the motor's ground leg.  The flyback diode catches the
inductive spike when the motor is switched off — without it the spike will
eventually punch through the 2N2222.

```
  5V rail ──────────────────────────── motor (+)
                                           │
                    ┌──[diode cathode]─────┘   ← flyback diode across motor
                    │  [diode anode]──┐
                                      │
                                  motor (–)
                                      │
                               [2N2222 Collector]
                                      │
  GPIO 4 ──[R1 1kΩ]──[2N2222 Base]   │
                        [2N2222 Emitter]
                                      │
                                     GND
```

**Base resistor check:**
- GPIO HIGH = 3.3V, V_BE ≈ 0.7V → I_base = (3.3 − 0.7) / 1000 = 2.6 mA
- Motor ~70mA, h_FE ≥ 100 → needs 0.7mA to saturate → 2.6mA ✓ fully saturated

---

## LED indicator

```
  GPIO 5 ──[R2 220Ω]──[LED anode]──[LED cathode]──GND
```

(3.3 − 2.0) / 220 ≈ 6 mA — comfortably within the GPIO 40mA limit.

---

## Decoupling capacitors

| Cap | Value  | Where                                             |
|:----|:-------|:--------------------------------------------------|
| C1  | 100µF  | Between 5V rail and GND, physically near motor    |
| C2  | 100nF  | Between DevKit VCC and GND, as close as possible  |

C1 absorbs the current surge when the motor starts.
C2 keeps the ESP32's supply stable during BLE radio bursts.

---

## Complete wiring at a glance

```
  ┌─ Power ─────────────────────────────────────────────────────────┐
  │  5× LR44 → LM2596 (5V) → DevKit VIN                           │
  │                        → motor (+)                             │
  │  GND throughout                                                 │
  └─────────────────────────────────────────────────────────────────┘

  ┌─ Motor ─────────────────────────────────────────────────────────┐
  │  GPIO4 → R1(1kΩ) → 2N2222 base                                │
  │  2N2222 collector → motor(–)                                   │
  │  2N2222 emitter   → GND                                        │
  │  motor(+)         → 5V                                         │
  │  diode: cathode → 5V,  anode → 2N2222 collector               │
  └─────────────────────────────────────────────────────────────────┘

  ┌─ LED ───────────────────────────────────────────────────────────┐
  │  GPIO5 → R2(220Ω) → LED(+) → LED(–) → GND                    │
  └─────────────────────────────────────────────────────────────────┘

  ┌─ Decoupling ────────────────────────────────────────────────────┐
  │  C1 100µF : 5V rail to GND  (near motor)                      │
  │  C2 100nF : DevKit VCC to GND  (close to chip)                │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Pin map

| Function    | GPIO | Direction | Firmware constant |
|:------------|:----:|:---------:|:------------------|
| Motor drive | 4    | Output    | `PIN_MOTOR`       |
| LED         | 5    | Output    | `PIN_LED`         |
