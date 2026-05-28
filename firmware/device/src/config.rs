// ── Hardware ──────────────────────────────────────────────────────────────────
// GPIO numbers for the ESP32-C3.  Override via features in Cargo.toml if
// targeting a different board.
pub const PIN_MOTOR: i32 = 4;   // drives transistor → ERM coin motor
pub const PIN_LED:   i32 = 5;   // status / vibration indicator LED

// ── BLE ───────────────────────────────────────────────────────────────────────
pub const DEVICE_NAME: &str = "KineticJewel";

// ── Connection timing ─────────────────────────────────────────────────────────
// Safety disconnect if the phone connects but never sends anything.
pub const IDLE_TIMEOUT_MS: u64 = 60_000;
