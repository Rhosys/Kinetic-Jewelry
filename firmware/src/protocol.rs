// ─────────────────────────────────────────────────────────────────────────────
// PROTOCOL STUB  –  replace this file when the real spec is ready
//
// This is the ONLY file that changes when the wire protocol is finalised.
// Everything else (BLE setup, hardware drivers, vibration engine) is stable.
//
// Defines:
//   - FIRMWARE_VERSION  – reported on the firmware characteristic so the phone
//                         knows what dialect to speak
//   - VibBlock          – a single timed motor action (on or off for N ms)
//   - Command           – what the device is being asked to do
//   - parse()           – raw BLE bytes → Command
// ─────────────────────────────────────────────────────────────────────────────

/// Version byte the device advertises on its firmware characteristic.
/// The phone reads this first and uses it to select compatible packet formats.
pub const FIRMWARE_VERSION: u8 = 1;

/// One timed step: motor on or off for a fixed duration.
#[derive(Debug, Clone, Copy)]
pub struct VibBlock {
    pub motor_on:    bool,
    pub duration_ms: u64,
}

/// Top-level command decoded from a BLE write.
#[derive(Debug)]
pub enum Command {
    /// Play `blocks` (in order) `repeat` times.
    Vibrate { blocks: Vec<VibBlock>, repeat: u8 },
    // Future commands land here; the rest of the firmware is unaffected.
}

#[derive(Debug)]
pub enum ParseError {
    TooShort,
    UnknownVersion(u8),
    UnknownCommand(u8),
}

/// Decode a raw BLE characteristic write into a [`Command`].
///
/// ── REPLACE THE BODY OF THIS FUNCTION when the real protocol is specified ──
///
/// The signature must stay the same; only the logic inside changes.
/// `data` is the raw byte slice received from the BLE write.
pub fn parse(data: &[u8]) -> Result<Command, ParseError> {
    // Placeholder wire format (TBD by protocol spec):
    //   byte 0  – protocol version
    //   byte 1  – command id  (0x01 = Vibrate)
    //   byte 2  – repeat count
    //   byte 3… – block ids (see decode_block below)
    if data.len() < 3 {
        return Err(ParseError::TooShort);
    }

    let version = data[0];
    let command = data[1];
    let repeat  = data[2].max(1);

    if version != FIRMWARE_VERSION {
        return Err(ParseError::UnknownVersion(version));
    }

    match command {
        0x01 => Ok(Command::Vibrate {
            blocks: data[3..].iter().filter_map(|&id| decode_block(id)).collect(),
            repeat,
        }),
        c => Err(ParseError::UnknownCommand(c)),
    }
}

// ── Block table ───────────────────────────────────────────────────────────────
// Map a block id byte to a (motor_on, duration_ms) pair.
// Unknown ids are skipped (None) so new blocks are forwards-compatible.
fn decode_block(id: u8) -> Option<VibBlock> {
    let (motor_on, duration_ms) = match id {
        0x01 => (true,  100),   // short buzz
        0x02 => (true,  250),   // medium buzz
        0x03 => (true,  500),   // long buzz
        0x04 => (false,  80),   // short pause
        0x05 => (false, 200),   // medium pause
        0x06 => (false, 600),   // long pause
        0x07 => (true,   40),   // click
        _    => return None,
    };
    Some(VibBlock { motor_on, duration_ms })
}
