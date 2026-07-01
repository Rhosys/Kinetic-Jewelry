# Bill of Materials – KineticJewel Charging Dock

A separate, passive charging base: takes USB-C power in and delivers it to
one or two devices via pogo-pin contacts. It has no MCU and does no charge
management itself — each device's own MCP73831 + DW01A/FS8205A circuit (see
`bom-production.md`) handles its own charge current and battery protection.
The dock's only job is to accept power from either USB-A→C or USB-C→C
cables and fan it out to two contact ports.

## USB-C power input

| Ref  | Part                          | Value / Spec                            | Qty | Notes                                                                 |
|:-----|:------------------------------|:------------------------------------------|:---:|:------------------------------------------------------------------------|
| J1   | USB-C receptacle               | 16-pin SMD (e.g. GCT USB4085 or equiv.)   | 1   | Power-only usage — data pins (D+/D−, SBU) left unconnected            |
| R1   | Resistor (CC1 pulldown)        | 5.1 kΩ, 0603                              | 1   | Advertises J1 as a USB-C sink (UFP) so USB-C chargers/hosts supply default 5V |
| R2   | Resistor (CC2 pulldown)        | 5.1 kΩ, 0603                              | 1   | Same as R1 — both CC pins must be pulled down independently            |
| F1   | PPTC resettable fuse            | 0603/1206, ~2A hold current               | 1   | Main input protection, sized for two ports drawing up to ~1A combined plus margin |
| C1   | Ceramic / tantalum cap          | 22 µF, 0805                               | 1   | Bulk decoupling on VBUS, feeds both ports                              |

### Why both USB-A→C and USB-C→C cables work

- **USB-A→C cable**: the host side has no CC pins at all, so it never
  negotiates — it just presents default 5V on VBUS the moment the cable is
  plugged in.
- **USB-C→C cable**: USB-C sources won't turn on VBUS until they detect a
  valid sink pulling CC1/CC2 low. **R1 and R2 are what make this path
  work** — without them, USB-C-only chargers/PD bricks see an open CC line
  and withhold power.

This is the same logic as the original single-device design — it just now
lives on the dock instead of on the jewelry piece.

## Dual charging ports

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

## Power budget for simultaneous charging

Each device charges at whatever its own R_PROG sets (default ~500mA per
`bom-production.md`). Charging two devices at once means the dock needs to
source **up to ~1A total** at 5V (10W) with headroom above that.

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

## Full charge path

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
