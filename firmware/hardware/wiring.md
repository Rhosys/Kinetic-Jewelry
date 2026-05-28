# Hardware Wiring – KineticJewel ESP32 BLE Device

## Recommended build: ESP32-C3 Super Mini + 3× LR44 + ERM coin motor

---

## Power supply

Three LR44 button cells in series give a nominal 4.5 V (fresh) that tapers to
~3.6 V as the cells discharge.  An AMS1117-3.3 LDO converts this cleanly to 3.3 V
for the ESP32-C3 and the LED.  The vibration motor is driven directly from the 4.5 V
rail for maximum vibration force.

```
  [LR44×3 in series]
  (+)4.5V ─────┬──────────────────────────── VIN (motor power)
               │
             [C1 100µF]            ← bulk capacitor, electrolytic
               │
               ├──[AMS1117-3.3 IN]──[AMS1117-3.3 ADJ/GND]──GND
               │         │
               │    [AMS1117-3.3 OUT]
               │         │
               │    [C2 10µF]──GND    ← output cap, electrolytic
               │         │
               │    [C3 100nF]──GND   ← decoupling, ceramic
               │         │
               │       3.3V ──────────── VCC (ESP32-C3, LED circuit)
               │
  (–) GND ─────┴──────────────────────── GND
```

> **Simpler alternative (2× LR44, no regulator):**
> Two LR44 in series = 3.0 V nominal.  The ESP32-C3 operates from 2.3 V–3.6 V, so
> this works without a regulator at the cost of slightly shorter battery life and
> reduced motor force.  Skip C1 and the AMS1117; connect the battery + directly to
> the ESP32-C3 VCC pin and to the motor.

---

## Vibration motor driver

An ERM coin vibration motor draws ~65–80 mA — more than any ESP32 GPIO can supply
directly.  An NPN transistor (2N2222A or BC337) switches the motor GND leg; the
motor's +V is tied to VIN (4.5 V) for full vibration force.

A flyback diode (1N4148) clamps the inductive kick when the motor is switched off.

```
  VIN (4.5V) ──── [Motor +]──[Motor –]
                                  │
                             [1N4148 anode]   } flyback diode
                                  │          }   cathode → VIN
                             [Collector]
                                  │
  GPIO4 ──── [R1 1kΩ] ──── [Base]  2N2222A / BC337
                                  │
                             [Emitter]
                                  │
                                 GND
```

**R1 (1 kΩ):** drives ≈2.6 mA into the base; saturates the transistor well above
the ~1 mA needed to switch 80 mA of collector current (hFE ≥ 100 for both parts).

---

## LED indicator

The LED lights whenever the motor vibrates (mirrors motor state).

```
  3.3V ─── [R2 220Ω] ─── [LED anode] ─── [LED cathode] ─── GND
                                │
                           GPIO5 drives this via software HIGH/LOW
```

Wait – GPIO5 drives the LED *directly* through a series resistor.  The LED is
simply tied to GPIO5 output through R2.

```
  GPIO5 ─── [R2 220Ω] ─── [LED anode] ─── [LED cathode] ─── GND
```

Forward current ≈ (3.3 V − 2.0 V) / 220 Ω ≈ 6 mA — safe for any 3 mm / 5 mm LED
and well within the ESP32 GPIO 40 mA limit.

---

## Complete pin map

| Signal       | ESP32-C3 GPIO | Direction | Notes                        |
|:-------------|:-------------:|:---------:|:-----------------------------|
| Motor switch | GPIO 4        | Output    | HIGH = transistor on = motor |
| LED          | GPIO 5        | Output    | HIGH = LED on                |
| Serial TX    | GPIO 21       | Output    | Debug console (115200 baud)  |
| Serial RX    | GPIO 20       | Input     | (unused, kept for flash)     |

> For ESP32-WROOM-32 boards set `PIN_MOTOR=25` and `PIN_LED=26` in
> `platformio.ini` `build_flags`.

---

## Full assembly diagram (text)

```
                 ┌─────────────────────────┐
  LR44×3 (+)────►│VIN                  GPIO4├────[1kΩ]────[NPN base]
                 │                         │              [NPN col]──[Motor–]
  LR44×3 (–)────►│GND                  GPIO5├────[220Ω]───[LED+]──[LED–]──GND
                 │                         │
  AMS1117 OUT───►│VCC   ESP32-C3           │
                 │      Super Mini         │
                 └─────────────────────────┘

  Motor+ ──── VIN (4.5V)
  1N4148: anode→NPN collector, cathode→VIN
  C1 100µF:  VIN to GND  (bulk)
  C2 10µF:   VCC to GND  (LDO output)
  C3 100nF:  VCC to GND  (ESP32 decoupling, place close to VCC pin)
```

---

## Power budget

| Component              | Typical current |
|:-----------------------|----------------:|
| ESP32-C3 BLE active    |        ~20 mA   |
| ESP32-C3 BLE peak      |        ~80 mA   |
| ERM coin motor (8 mm)  |        ~70 mA   |
| LED (6 mA, on w/motor) |         ~6 mA   |
| AMS1117 quiescent      |         ~5 mA   |
| **Worst-case peak**    |    **~161 mA**  |

Three LR44 cells (150 mAh each, but internal resistance limits burst current —
consider them ≈100 mAh effective in this circuit).  Continuous worst-case: ~1.5 h.
In practice the motor and BLE transmitter are only briefly active; expect many hours
of real-world use between battery changes.
