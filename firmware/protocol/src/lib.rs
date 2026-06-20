// ─────────────────────────────────────────────────────────────────────────────
// PROTOCOL  –  the only layer that changes when the wire spec is finalised
//
// Pure Rust, no esp-idf, no hardware dependencies. Host-testable with
// `cargo test`. The firmware binary (../device) depends on this crate by path.
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
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct VibBlock {
    pub motor_on:    bool,
    pub duration_ms: u64,
}

/// Top-level command decoded from a BLE write.
#[derive(Debug, PartialEq, Eq)]
pub enum Command {
    /// Play `blocks` (in order) `repeat` times.
    Vibrate { blocks: Vec<VibBlock>, repeat: u8 },
    // Future commands land here; the rest of the firmware is unaffected.
}

#[derive(Debug, PartialEq, Eq)]
pub enum ParseError {
    TooShort,
    UnknownVersion(u8),
    UnknownCommand(u8),
}

/// Decode a raw BLE characteristic write into a [`Command`].
///
/// Wire format:
///   byte 0  – protocol version  (must equal FIRMWARE_VERSION)
///   byte 1  – command id        (0x01 = Vibrate)
///   byte 2  – repeat count      (clamped to 1 if 0)
///   byte 3… – block ids         (see decode_block; unknown ids are skipped)
pub fn parse(data: &[u8]) -> Result<Command, ParseError> {
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
pub fn decode_block(id: u8) -> Option<VibBlock> {
    let (motor_on, duration_ms) = match id {
        0x01 => (true,  100),   // short buzz
        0x02 => (true,  250),   // medium buzz
        0x03 => (true,  500),   // long buzz
        0x04 => (false,  80),   // short pause
        0x05 => (false, 200),   // medium pause
        0x06 => (false, 600),   // long pause
        0x07 => (true,   40),   // click
        0x08 => (true, 1000),   // extra-long buzz (escalating finale)
        _    => return None,
    };
    Some(VibBlock { motor_on, duration_ms })
}

// ─────────────────────────────────────────────────────────────────────────────
// Tests – run with `cargo test` from firmware/protocol (host, no hardware)
// ─────────────────────────────────────────────────────────────────────────────
#[cfg(test)]
mod tests {
    use super::*;

    fn buzz(d: u64) -> VibBlock  { VibBlock { motor_on: true,  duration_ms: d } }
    fn pause(d: u64) -> VibBlock { VibBlock { motor_on: false, duration_ms: d } }

    #[test]
    fn valid_vibrate_packet_decodes_blocks_and_repeat() {
        // version 1, cmd vibrate, repeat 2, [short_buzz, short_pause, long_buzz]
        let pkt = [0x01, 0x01, 0x02, 0x01, 0x04, 0x03];
        let cmd = parse(&pkt).unwrap();
        assert_eq!(
            cmd,
            Command::Vibrate {
                blocks: vec![buzz(100), pause(80), buzz(500)],
                repeat: 2,
            }
        );
    }

    #[test]
    fn header_only_packet_yields_empty_blocks() {
        let cmd = parse(&[0x01, 0x01, 0x01]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![], repeat: 1 });
    }

    #[test]
    fn packet_shorter_than_header_is_too_short() {
        assert_eq!(parse(&[]),            Err(ParseError::TooShort));
        assert_eq!(parse(&[0x01]),        Err(ParseError::TooShort));
        assert_eq!(parse(&[0x01, 0x01]),  Err(ParseError::TooShort));
    }

    #[test]
    fn wrong_version_is_rejected() {
        assert_eq!(parse(&[0x99, 0x01, 0x01, 0x01]),
                   Err(ParseError::UnknownVersion(0x99)));
    }

    #[test]
    fn unknown_command_is_rejected() {
        assert_eq!(parse(&[0x01, 0x42, 0x01, 0x01]),
                   Err(ParseError::UnknownCommand(0x42)));
    }

    #[test]
    fn repeat_zero_is_clamped_to_one() {
        let cmd = parse(&[0x01, 0x01, 0x00, 0x01]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![buzz(100)], repeat: 1 });
    }

    #[test]
    fn unknown_block_ids_are_skipped() {
        // 0xFF is not a known block; it must be filtered, not crash.
        let cmd = parse(&[0x01, 0x01, 0x01, 0x01, 0xFF, 0x03]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![buzz(100), buzz(500)], repeat: 1 });
    }

    #[test]
    fn every_known_block_maps_to_expected_timing() {
        assert_eq!(decode_block(0x01), Some(buzz(100)));   // short buzz
        assert_eq!(decode_block(0x02), Some(buzz(250)));   // medium buzz
        assert_eq!(decode_block(0x03), Some(buzz(500)));   // long buzz
        assert_eq!(decode_block(0x04), Some(pause(80)));   // short pause
        assert_eq!(decode_block(0x05), Some(pause(200)));  // medium pause
        assert_eq!(decode_block(0x06), Some(pause(600)));  // long pause
        assert_eq!(decode_block(0x07), Some(buzz(40)));    // click
        assert_eq!(decode_block(0x08), Some(buzz(1000)));  // extra-long buzz
        assert_eq!(decode_block(0x00), None);
        assert_eq!(decode_block(0xFF), None);
    }

    #[test]
    fn pause_blocks_keep_motor_off() {
        for id in [0x04u8, 0x05, 0x06] {
            assert!(!decode_block(id).unwrap().motor_on, "block {id:#x} should be off");
        }
        for id in [0x01u8, 0x02, 0x03, 0x07, 0x08] {
            assert!(decode_block(id).unwrap().motor_on, "block {id:#x} should be on");
        }
    }
}
