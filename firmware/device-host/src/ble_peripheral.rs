use anyhow::Result;
use bluer::{
    adv::{Advertisement, AdvertisementHandle},
    gatt::local::{
        Application, ApplicationHandle, Characteristic, CharacteristicRead,
        CharacteristicWrite, CharacteristicWriteMethod, Service,
    },
    Adapter, Uuid,
};
use futures::FutureExt;
use log::info;

use kinetic_protocol::{self as protocol, FIRMWARE_VERSION};

use crate::{
    config::{CHAR_COMMAND, CHAR_FIRMWARE, DEVICE_NAME, SVC_UUID},
    vibration::{self, Queue},
};

/// Returned to the caller; drop to stop advertising and unregister the GATT app.
pub struct PeripheralHandles {
    pub _app: ApplicationHandle,
    pub _adv: AdvertisementHandle,
}

/// Register a GATT application on `adapter` and start advertising.
///
/// The application mirrors `firmware/device/src/ble.rs`:
///   - Firmware char (READ) → returns FIRMWARE_VERSION byte
///   - Command char (WRITE) → calls protocol::parse() then vibration::enqueue()
pub async fn start(adapter: &Adapter, queue: Queue) -> Result<PeripheralHandles> {
    let svc_uuid      = Uuid::parse_str(SVC_UUID)?;
    let firmware_uuid = Uuid::parse_str(CHAR_FIRMWARE)?;
    let command_uuid  = Uuid::parse_str(CHAR_COMMAND)?;

    // Firmware version characteristic — READ
    let firmware_char = Characteristic {
        uuid: firmware_uuid,
        read: Some(CharacteristicRead {
            read: true,
            fun: Box::new(|_req| {
                async move { Ok(vec![FIRMWARE_VERSION]) }.boxed()
            }),
            ..Default::default()
        }),
        ..Default::default()
    };

    // Command characteristic — WRITE (with response)
    let q = queue.clone();
    let command_char = Characteristic {
        uuid: command_uuid,
        write: Some(CharacteristicWrite {
            write: true,
            method: CharacteristicWriteMethod::Fun(Box::new(move |data, _req| {
                let q = q.clone();
                async move {
                    match protocol::parse(&data) {
                        Ok(protocol::Command::Vibrate { blocks, repeat }) => {
                            info!("[BLE] vibrate {} block(s) × {}", blocks.len(), repeat);
                            vibration::enqueue(&q, &blocks, repeat);
                            Ok(())
                        }
                        Err(e) => {
                            log::warn!("[BLE] parse error: {:?}", e);
                            Err(bluer::gatt::local::ReqError::NotSupported)
                        }
                    }
                }
                .boxed()
            })),
            ..Default::default()
        }),
        ..Default::default()
    };

    let app = Application {
        services: vec![Service {
            uuid: svc_uuid,
            primary: true,
            characteristics: vec![firmware_char, command_char],
            ..Default::default()
        }],
        ..Default::default()
    };

    let app_handle = adapter.serve_gatt_application(app).await?;
    info!("[BLE] GATT application registered on {}", adapter.name());

    let adv = Advertisement {
        advertisement_type: bluer::adv::Type::Peripheral,
        service_uuids: [svc_uuid].into(),
        discoverable: Some(true),
        local_name: Some(DEVICE_NAME.to_string()),
        ..Default::default()
    };
    let adv_handle = adapter.advertise(adv).await?;
    info!("[BLE] advertising as \"{}\"", DEVICE_NAME);

    Ok(PeripheralHandles { _app: app_handle, _adv: adv_handle })
}
