use anyhow::Result;
use kinetic_jewel_host::{ble_peripheral, mock_gpio::MockGpio, vibration};

/// Manual smoke-test binary.
/// Run with: cargo run --bin kinetic-jewel-host
/// Requires BlueZ + a real or virtual HCI adapter.
#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();

    let session = bluer::Session::new().await?;
    let adapter = session.default_adapter().await?;
    adapter.set_powered(true).await?;

    log::info!("Using adapter {} ({})", adapter.name(), adapter.address().await?);

    let motor = MockGpio::new();
    let led   = MockGpio::new();
    let queue = vibration::new_queue();

    vibration::run_thread(queue.clone(), motor.clone(), led.clone());
    let _handles = ble_peripheral::start(&adapter, queue).await?;

    log::info!("Peripheral running. Press Ctrl-C to stop.");
    tokio::signal::ctrl_c().await?;
    Ok(())
}
