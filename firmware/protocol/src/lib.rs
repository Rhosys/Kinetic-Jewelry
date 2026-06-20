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
///   byte 3… – packed block ids  – two 4-bit ids per byte, high nibble first.
///             See decode_block; unknown nibbles (including the 0x0 used to
///             pad an odd-length sequence) are skipped.
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
            blocks: data[3..]
                .iter()
                .flat_map(|&byte| [byte >> 4, byte & 0x0F])
                .filter_map(decode_block)
                .collect(),
            repeat,
        }),
        c => Err(ParseError::UnknownCommand(c)),
    }
}

// ── Block table ───────────────────────────────────────────────────────────────
// Map a 4-bit block id nibble to a (motor_on, duration_ms) pair.
// 0x1-0x9 are vibrations (motor on), 0xA-0xF are pauses (motor off).
// Unknown ids are skipped (None) so new blocks are forwards-compatible.
pub fn decode_block(id: u8) -> Option<VibBlock> {
    let (motor_on, duration_ms) = match id {
        0x1 => (true,   40),   // click
        0x2 => (true,  100),   // short buzz
        0x3 => (true,  250),   // medium buzz
        0x4 => (true,  500),   // long buzz
        0x5 => (true, 1000),   // extra-long buzz (escalating finale)
        0xA => (false,  80),   // short pause
        0xB => (false, 200),   // medium pause
        0xC => (false, 600),   // long pause
        _   => return None,
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
        // packed: (short_buzz=0x2, short_pause=0xA) -> 0x2A, (long_buzz=0x4, pad) -> 0x40
        let pkt = [0x01, 0x01, 0x02, 0x2A, 0x40];
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
        assert_eq!(parse(&[0x99, 0x01, 0x01, 0x20]),
                   Err(ParseError::UnknownVersion(0x99)));
    }

    #[test]
    fn unknown_command_is_rejected() {
        assert_eq!(parse(&[0x01, 0x42, 0x01, 0x20]),
                   Err(ParseError::UnknownCommand(0x42)));
    }

    #[test]
    fn repeat_zero_is_clamped_to_one() {
        // 0x01 unpacks to nibbles [0x0, 0x1] -> 0x0 is padding (skipped), 0x1 is click
        let cmd = parse(&[0x01, 0x01, 0x00, 0x01]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![buzz(40)], repeat: 1 });
    }

    #[test]
    fn unknown_block_ids_are_skipped() {
        // 0x2F -> short_buzz(0x2) + unknown nibble 0xF (skipped)
        // 0x4F -> long_buzz(0x4) + unknown nibble 0xF (skipped)
        let cmd = parse(&[0x01, 0x01, 0x01, 0x2F, 0x4F]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![buzz(100), buzz(500)], repeat: 1 });
    }

    #[test]
    fn every_known_block_maps_to_expected_timing() {
        assert_eq!(decode_block(0x1), Some(buzz(40)));     // click
        assert_eq!(decode_block(0x2), Some(buzz(100)));    // short buzz
        assert_eq!(decode_block(0x3), Some(buzz(250)));    // medium buzz
        assert_eq!(decode_block(0x4), Some(buzz(500)));    // long buzz
        assert_eq!(decode_block(0x5), Some(buzz(1000)));   // extra-long buzz
        assert_eq!(decode_block(0xA), Some(pause(80)));    // short pause
        assert_eq!(decode_block(0xB), Some(pause(200)));   // medium pause
        assert_eq!(decode_block(0xC), Some(pause(600)));   // long pause
        assert_eq!(decode_block(0x0), None);
        assert_eq!(decode_block(0x9), None);
        assert_eq!(decode_block(0xF), None);
    }

    #[test]
    fn pause_blocks_keep_motor_off() {
        for id in [0xAu8, 0xB, 0xC] {
            assert!(!decode_block(id).unwrap().motor_on, "block {id:#x} should be off");
        }
        for id in [0x1u8, 0x2, 0x3, 0x4, 0x5] {
            assert!(decode_block(id).unwrap().motor_on, "block {id:#x} should be on");
        }
    }

    #[test]
    fn odd_length_sequence_is_padded_with_a_skipped_nibble() {
        // single click (0x1) packed alone -> high nibble 0x1, low nibble 0x0 (pad)
        let cmd = parse(&[0x01, 0x01, 0x01, 0x10]).unwrap();
        assert_eq!(cmd, Command::Vibrate { blocks: vec![buzz(40)], repeat: 1 });
    }
}
