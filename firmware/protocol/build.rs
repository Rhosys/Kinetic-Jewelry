// Generates BLE UUID constants from the repo-root `ble-protocol.json`, so the
// firmware and the Kotlin app read the exact same file instead of hand-copied
// string literals drifting apart.
use std::{env, fs, path::Path};

fn main() {
    let manifest_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let json_path = Path::new(&manifest_dir).join("../../ble-protocol.json");
    println!("cargo:rerun-if-changed={}", json_path.display());

    let json = fs::read_to_string(&json_path)
        .unwrap_or_else(|e| panic!("cannot read {}: {e}", json_path.display()));
    let parsed: serde_json::Value = serde_json::from_str(&json)
        .unwrap_or_else(|e| panic!("cannot parse {}: {e}", json_path.display()));

    let field = |name: &str| -> String {
        parsed[name]
            .as_str()
            .unwrap_or_else(|| panic!("{}: missing string field `{name}`", json_path.display()))
            .to_owned()
    };

    let generated = format!(
        "pub const SVC_UUID: &str = {:?};\n\
         pub const CHAR_FIRMWARE_UUID: &str = {:?};\n\
         pub const CHAR_COMMAND_UUID: &str = {:?};\n",
        field("service_uuid"),
        field("firmware_characteristic_uuid"),
        field("command_characteristic_uuid"),
    );

    let out_dir = env::var("OUT_DIR").unwrap();
    fs::write(Path::new(&out_dir).join("ble_uuids.rs"), generated).unwrap();
}
