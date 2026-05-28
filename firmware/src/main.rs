use esp_idf_hal::peripherals::Peripherals;
use esp_idf_svc::eventloop::EspSystemEventLoop;
use log::info;

mod ble;
mod config;
mod protocol;
mod vibration;

fn main() -> anyhow::Result<()> {
    esp_idf_svc::sys::link_patches();
    esp_idf_svc::log::EspLogger::initialize_default();

    info!("[BOOT] KineticJewel – protocol v{}", protocol::FIRMWARE_VERSION);

    let peripherals = Peripherals::take()?;
    let _sysloop    = EspSystemEventLoop::take()?;

    // Vibration engine – runs on its own thread, owns the GPIO pins.
    let queue = vibration::new_queue();
    vibration::run_thread(
        queue.clone(),
        peripherals.pins.gpio4.into(),
        peripherals.pins.gpio5.into(),
    );

    // BLE GATT server – starts advertising immediately.
    ble::start(queue)?;

    // Keep the main task alive; all real work happens in the BLE + vib threads.
    loop {
        std::thread::sleep(std::time::Duration::from_secs(60));
    }
}
