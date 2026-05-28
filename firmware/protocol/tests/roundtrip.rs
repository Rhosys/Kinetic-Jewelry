// Round-trip integration test: reads the canonical test-vectors.json and
// verifies that protocol::parse() agrees with every case.
//
// Run with: cargo test --test roundtrip

use kinetic_protocol::{parse, Command};
use serde::Deserialize;
use std::fs;

// ── JSON schema ───────────────────────────────────────────────────────────────

#[derive(Deserialize)]
struct Vectors {
    cases: Vec<Case>,
}

#[derive(Deserialize)]
struct Case {
    id:             String,
    bytes_hex:      String,
    repeat:         u8,
    decoded_blocks: Vec<BlockSpec>,
}

#[derive(Deserialize)]
struct BlockSpec {
    motor_on:    bool,
    duration_ms: u64,
}

// ── Helper ────────────────────────────────────────────────────────────────────

fn load_vectors() -> Vectors {
    let path = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/test-vectors.json"
    );
    let json = fs::read_to_string(path)
        .unwrap_or_else(|e| panic!("cannot read test-vectors.json: {e}"));
    serde_json::from_str(&json)
        .unwrap_or_else(|e| panic!("cannot parse test-vectors.json: {e}"))
}

fn parse_hex(s: &str) -> Vec<u8> {
    s.split_whitespace()
        .map(|h| u8::from_str_radix(h, 16).unwrap_or_else(|_| panic!("bad hex: {h}")))
        .collect()
}

// ── Test ──────────────────────────────────────────────────────────────────────

#[test]
fn all_vectors_parse_correctly() {
    let vectors = load_vectors();

    for case in &vectors.cases {
        let bytes = parse_hex(&case.bytes_hex);

        let cmd = parse(&bytes).unwrap_or_else(|e| {
            panic!("case '{}': parse() returned Err({e:?})", case.id)
        });

        let Command::Vibrate { blocks, repeat } = cmd;

        assert_eq!(
            repeat,
            case.repeat,
            "case '{}': expected repeat={}, got {repeat}",
            case.id,
            case.repeat,
        );

        assert_eq!(
            blocks.len(),
            case.decoded_blocks.len(),
            "case '{}': expected {} blocks, got {}",
            case.id,
            case.decoded_blocks.len(),
            blocks.len(),
        );

        for (i, (got, want)) in blocks.iter().zip(&case.decoded_blocks).enumerate() {
            assert_eq!(
                got.motor_on, want.motor_on,
                "case '{}' block {i}: motor_on expected={} got={}",
                case.id, want.motor_on, got.motor_on,
            );
            assert_eq!(
                got.duration_ms, want.duration_ms,
                "case '{}' block {i}: duration_ms expected={} got={}",
                case.id, want.duration_ms, got.duration_ms,
            );
        }
    }
}
