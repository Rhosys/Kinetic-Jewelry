# KineticJewel – Charging System Diagram

Full power/charging path across all three BOMs: the dock
(`bom-charging-dock.md`) and two devices (`bom-production.md`), from the
USB-C plug down to the motor and status LEDs. Ref designators match the
tables in those files — each subgraph reuses its own file's refs (e.g. both
devices have their own `U1`, `J1`, etc.), so treat the subgraph boundary as
the scope for any ref, not the whole diagram.

```mermaid
flowchart TB
    subgraph DOCK["Charging Dock — bom-charging-dock.md"]
        direction TB
        USBC["J1<br/>USB-C receptacle"] -->|"VBUS"| FMAIN["F1<br/>PPTC ~2A"]
        USBC -->|"CC1"| RCC1["R1<br/>5.1k"] --> DGND(("GND"))
        USBC -->|"CC2"| RCC2["R2<br/>5.1k"] --> DGND
        USBC -->|"GND"| DGND
        FMAIN --> DRAIL(("Shared VBUS rail"))
        C1D["C1<br/>22uF"] --- DRAIL
        C1D --- DGND
        DRAIL --> FA["F2<br/>PPTC ~750mA"]
        DRAIL --> FB["F3<br/>PPTC ~750mA"]
        FA --> PORTA["J2<br/>Pogo pins — Port A"]
        FB --> PORTB["J3<br/>Pogo pins — Port B"]
        DRAIL --> LEDDA["LED1<br/>Port A present"] --> DGND
        DRAIL --> LEDDB["LED2<br/>Port B present"] --> DGND
    end

    subgraph DEVA["Device A — bom-production.md"]
        direction TB
        PADA["J1<br/>Pogo pad"] --> FUSEA["F1<br/>PPTC ~750mA"]
        FUSEA --> CHGA["U2<br/>MCP73831"]
        CHGA <--> PROTA["U3<br/>DW01A+FS8205A"]
        PROTA <--> BATA["BT1<br/>Li-Po 3.7V"]
        BATA --> LDOA["U4<br/>AP2112K-3.3"]
        LDOA --> RAILA(("3.3V rail"))
        RAILA --> MCUA["U1<br/>ESP32-C3-MINI-1"]
        MCUA -->|"GPIO"| MOTA["Q1+D1 driver<br/>→ M1 motor"]
        MCUA -->|"GPIO"| LEDA1["LED1<br/>status"]
        CHGA -->|"STAT"| LEDA2["LED2<br/>charge status"]
    end

    subgraph DEVB["Device B — identical to Device A"]
        direction TB
        PADB["J1<br/>Pogo pad"] --> FUSEB["F1<br/>PPTC ~750mA"]
        FUSEB --> CHGB["U2<br/>MCP73831"]
        CHGB <--> PROTB["U3<br/>DW01A+FS8205A"]
        PROTB <--> BATB["BT1<br/>Li-Po 3.7V"]
        BATB --> LDOB["U4<br/>AP2112K-3.3"]
        LDOB --> RAILB(("3.3V rail"))
        RAILB --> MCUB["U1<br/>ESP32-C3-MINI-1"]
        MCUB -->|"GPIO"| MOTB["Q1+D1 driver<br/>→ M1 motor"]
        MCUB -->|"GPIO"| LEDB1["LED1<br/>status"]
        CHGB -->|"STAT"| LEDB2["LED2<br/>charge status"]
    end

    PORTA -.->|"mates with"| PADA
    PORTB -.->|"mates with"| PADB

    classDef net fill:#fffbe6,stroke:#b59f00,stroke-width:2px;
    class DGND,DRAIL,RAILA,RAILB net;
```

Notes:
- Only the two pogo-pin mating connections (dotted arrows) cross the
  subgraph boundaries — the dock and each device are otherwise fully
  independent circuits, which is why either device can dock into either
  port, and either port can be empty without affecting the other.
- Charge current/termination/protection (`U2`/`U3`) live entirely on each
  device, not the dock — see the "Full charge path" sections in
  `bom-production.md` and `bom-charging-dock.md` for the prose walkthrough.
- BLE control flow (phone ↔ motor/LED once running on battery) is a
  separate concern from charging and is already diagrammed in
  `wiring.md` (applies unchanged to the production BOM's `U1`).
