pub const DEVICE_NAME: &str = "KineticJewel";

// BLE UUIDs come from kinetic-protocol (generated from the repo-root
// ble-protocol.json), not a hand-copied mirror of firmware/device.
pub use kinetic_protocol::{
    CHAR_COMMAND_UUID as CHAR_COMMAND, CHAR_FIRMWARE_UUID as CHAR_FIRMWARE, SVC_UUID,
};
