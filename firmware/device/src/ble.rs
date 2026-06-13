use esp32_nimble::{uuid128, BLEAdvertisementData, BLEDevice, NimBLEProperties};
use log::info;

use crate::{protocol, vibration};

// ── BLE UUIDs ─────────────────────────────────────────────────────────────────
// Service
const SVC: &str          = "6b2f0001-0000-1000-8000-00805f9b34fb";
// Firmware version – phone READs this first to learn the protocol version
const CHAR_FIRMWARE: &str = "6b2f0004-0000-1000-8000-00805f9b34fb";
// Command – phone WRITEs vibration packets here
const CHAR_COMMAND: &str  = "6b2f0002-0000-1000-8000-00805f9b34fb";

pub fn start(queue: vibration::Queue) -> anyhow::Result<()> {
    let ble    = BLEDevice::take();
    let server = ble.get_server();

    server.on_connect(|_server, desc| {
        info!("[BLE] connected (handle {})", desc.conn_handle());
    });
    server.on_disconnect(|desc, reason| {
        info!("[BLE] disconnected (handle {} reason {})", desc.conn_handle(), reason);
        // NimBLE restarts advertising automatically after disconnect.
    });

    {
        let svc = server.create_service(uuid128!(SVC));
        let mut svc = svc.lock();

        // Firmware characteristic – READ
        svc.create_characteristic(uuid128!(CHAR_FIRMWARE), NimBLEProperties::READ)
            .lock()
            .set_value(&[protocol::FIRMWARE_VERSION]);

        // Command characteristic – WRITE WITH RESPONSE
        // (with-response means the phone gets a confirmation; helps the phone
        //  sequence multi-packet patterns correctly once the protocol is ready)
        let cmd = svc.create_characteristic(
            uuid128!(CHAR_COMMAND),
            NimBLEProperties::WRITE,
        );

        cmd.lock().on_write(move |args| {
            match protocol::parse(args.recv_data()) {
                Ok(protocol::Command::Vibrate { blocks, repeat }) => {
                    info!("[BLE] vibrate {} block(s) × {}", blocks.len(), repeat);
                    vibration::enqueue(&queue, &blocks, repeat);
                }
                Err(e) => log::warn!("[BLE] parse error: {:?}", e),
            }
        });
    }

    let adv = ble.get_advertising();
    adv.lock().set_data(
        BLEAdvertisementData::new()
            .name(crate::config::DEVICE_NAME)
            .add_service_uuid(uuid128!(SVC)),
    )?;
    adv.lock().start()?;

    info!("[BLE] advertising as \"{}\"", crate::config::DEVICE_NAME);
    Ok(())
}
