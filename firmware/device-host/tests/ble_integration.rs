// Suite A — Device BLE integration tests
//
// Requires:
//   sudo modprobe hci_vhci      (virtual HCI kernel module)
//   sudo systemctl start bluetooth
//   hci0 powered on
//
// Run manually:
//   sudo -E env "PATH=$PATH" cargo test --test ble_integration -- --nocapture
//
// Each test spins up the GATT peripheral on hci0, then connects to it as a
// GATT central on the same adapter (BlueZ loopback via virtual HCI), exercises
// one or more characteristics, and asserts on MockGpio events.

use std::collections::BTreeSet;
use std::time::{Duration, Instant};

use anyhow::{Context, Result};
use bluer::{AdapterEvent, Device, Uuid};
use futures::StreamExt;
use kinetic_jewel_host::{
    ble_peripheral,
    config::{CHAR_COMMAND, CHAR_FIRMWARE, DEVICE_NAME, SVC_UUID},
    mock_gpio::MockGpio,
    vibration::{self, wait_for_idle, Queue},
};

// ── Timing tolerance ──────────────────────────────────────────────────────────
// Virtual HCI + thread scheduling: ±100 ms is generous but avoids false
// positives in CI where the scheduler may lag.
const TIMING_TOLERANCE_MS: i64 = 100;

// ── Test harness ──────────────────────────────────────────────────────────────

struct Harness {
    pub motor: MockGpio,
    pub led:   MockGpio,
    pub queue: Queue,
    // Peripheral handles kept alive for the duration of the test.
    _peripheral: ble_peripheral::PeripheralHandles,
    // Device handle for the central connection.
    device: Device,
}

impl Harness {
    /// Start peripheral + connect central on the same adapter.
    /// Returns Err (test skipped) if no virtual adapter is available.
    async fn new() -> Result<Self> {
        let session = bluer::Session::new().await?;

        let adapter = match session.default_adapter().await {
            Ok(a) => a,
            Err(_) => {
                anyhow::bail!("SKIP: no Bluetooth adapter available (hci_vhci not loaded?)");
            }
        };
        adapter.set_powered(true).await.context("adapter power on")?;

        let motor = MockGpio::new();
        let led   = MockGpio::new();
        let queue = vibration::new_queue();
        vibration::run_thread(queue.clone(), motor.clone(), led.clone());

        let peripheral = ble_peripheral::start(&adapter, queue.clone()).await
            .context("start peripheral")?;

        // Small delay to let BlueZ propagate the advertisement.
        tokio::time::sleep(Duration::from_millis(200)).await;

        let device = connect_central(&adapter).await
            .context("central connect")?;

        Ok(Self { motor, led, queue, _peripheral: peripheral, device })
    }

    /// Find the command characteristic and write a raw packet to it.
    pub async fn write_command(&self, bytes: &[u8]) -> Result<()> {
        let ch = find_char(&self.device, CHAR_COMMAND).await?;
        ch.write(bytes).await.context("characteristic write")
    }

    /// Find the firmware characteristic and read its value.
    pub async fn read_firmware(&self) -> Result<Vec<u8>> {
        let ch = find_char(&self.device, CHAR_FIRMWARE).await?;
        ch.read().await.context("characteristic read")
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/// Scan on `adapter` until a device named DEVICE_NAME is found, then connect.
async fn connect_central(adapter: &bluer::Adapter) -> Result<Device> {
    let svc_uuid = Uuid::parse_str(SVC_UUID)?;

    let mut discovery = adapter.discover_devices_with_changes().await?;

    let deadline = tokio::time::Instant::now() + Duration::from_secs(15);

    loop {
        if tokio::time::Instant::now() > deadline {
            anyhow::bail!("peripheral not found within 15 s");
        }

        let event = tokio::time::timeout(
            Duration::from_secs(1),
            discovery.next(),
        ).await;

        match event {
            Ok(Some(AdapterEvent::DeviceAdded(addr))) => {
                let dev = adapter.device(addr)?;
                if dev.name().await?.as_deref() == Some(DEVICE_NAME) {
                    dev.connect().await.context("connect")?;
                    // Wait for service discovery to complete.
                    tokio::time::sleep(Duration::from_millis(300)).await;
                    return Ok(dev);
                }
            }
            Ok(Some(AdapterEvent::DeviceChanged(addr))) => {
                let dev = adapter.device(addr)?;
                if dev.name().await?.as_deref() == Some(DEVICE_NAME)
                    && dev.is_connected().await?
                {
                    return Ok(dev);
                }
            }
            _ => {}
        }
    }
}

/// Find a GATT characteristic by UUID string on a connected device.
async fn find_char(
    device: &Device,
    uuid_str: &str,
) -> Result<bluer::gatt::remote::Characteristic> {
    let target = Uuid::parse_str(uuid_str)?;
    let svc_uuid = Uuid::parse_str(SVC_UUID)?;

    for service in device.services().await? {
        if service.uuid().await? == svc_uuid {
            for ch in service.characteristics().await? {
                if ch.uuid().await? == target {
                    return Ok(ch);
                }
            }
        }
    }
    anyhow::bail!("characteristic {} not found", uuid_str)
}

/// Assert that the duration between events[idx] and events[idx+1] is within
/// tolerance of `expected_ms`.
fn assert_duration(events: &[(Instant, bool)], idx: usize, expected_ms: u64, label: &str) {
    let delta = events[idx + 1].0.duration_since(events[idx].0);
    let diff = (delta.as_millis() as i64 - expected_ms as i64).abs();
    assert!(
        diff < TIMING_TOLERANCE_MS,
        "{}: expected ~{}ms, got {:?} (diff {}ms)",
        label, expected_ms, delta, diff
    );
}

// ── Test 1: Advertises ────────────────────────────────────────────────────────

#[tokio::test]
async fn test_01_advertises() -> Result<()> {
    let session = bluer::Session::new().await?;
    let adapter = match session.default_adapter().await {
        Ok(a) => a,
        Err(_) => {
            eprintln!("SKIP test_01_advertises: no adapter");
            return Ok(());
        }
    };
    adapter.set_powered(true).await?;

    let queue = vibration::new_queue();
    let motor = MockGpio::new();
    let led   = MockGpio::new();
    vibration::run_thread(queue.clone(), motor, led);
    let _peripheral = ble_peripheral::start(&adapter, queue).await?;

    tokio::time::sleep(Duration::from_millis(200)).await;

    let svc_uuid = Uuid::parse_str(SVC_UUID)?;
    let mut discovery = adapter.discover_devices_with_changes().await?;
    let found = tokio::time::timeout(Duration::from_secs(10), async {
        while let Some(evt) = discovery.next().await {
            if let AdapterEvent::DeviceAdded(addr) = evt {
                let dev = adapter.device(addr)?;
                if dev.name().await?.as_deref() == Some(DEVICE_NAME) {
                    let uuids: BTreeSet<_> = dev.uuids().await?.unwrap_or_default();
                    assert!(uuids.contains(&svc_uuid), "service UUID not in advertisement");
                    return Ok::<_, anyhow::Error>(true);
                }
            }
        }
        Ok(false)
    })
    .await
    .unwrap_or(Ok(false))?;

    assert!(found, "KineticJewel not found in scan");
    Ok(())
}

// ── Test 2: Connects ──────────────────────────────────────────────────────────

#[tokio::test]
async fn test_02_connects() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => {
            eprintln!("{}", e);
            return Ok(());
        }
        Err(e) => return Err(e),
    };

    assert!(
        h.device.is_connected().await?,
        "device is not connected after Harness::new()"
    );

    let svc_uuid = Uuid::parse_str(SVC_UUID)?;
    let service_found = h.device.services().await?.iter().any(|_| true);
    assert!(service_found, "no GATT services discovered");

    let _ = h; // drop → disconnect
    Ok(())
}

// ── Test 3: Firmware version ──────────────────────────────────────────────────

#[tokio::test]
async fn test_03_firmware_version() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    let value = h.read_firmware().await?;
    assert_eq!(value, vec![kinetic_protocol::FIRMWARE_VERSION], "wrong firmware version byte");
    Ok(())
}

// ── Test 4: Single block ──────────────────────────────────────────────────────

#[tokio::test]
async fn test_04_single_block() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // [version=1, cmd=vibrate, repeat=1, block=short_buzz(100ms)]
    h.write_command(&[0x01, 0x01, 0x01, 0x01]).await?;

    assert!(
        wait_for_idle(&h.queue, Duration::from_secs(2)),
        "queue did not drain"
    );
    tokio::time::sleep(Duration::from_millis(30)).await; // let motor go off

    let events = h.motor.events();
    assert!(events.len() >= 2, "expected at least 2 GPIO events, got {}", events.len());
    assert!(events[0].1, "first event should be HIGH (motor on)");
    assert!(!events.last().unwrap().1, "last event should be LOW (motor off)");
    assert_duration(&events, 0, 100, "single short buzz");
    Ok(())
}

// ── Test 5: Repeat ────────────────────────────────────────────────────────────

#[tokio::test]
async fn test_05_repeat() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // short_buzz × 3
    h.write_command(&[0x01, 0x01, 0x03, 0x01]).await?;

    assert!(wait_for_idle(&h.queue, Duration::from_secs(3)), "queue did not drain");
    tokio::time::sleep(Duration::from_millis(30)).await;

    let events = h.motor.events();
    // 3 × (HIGH + LOW) = 6 events minimum
    assert!(events.len() >= 6, "expected ≥6 events for 3 repeats, got {}", events.len());

    // Verify each HIGH→LOW transition is ~100 ms
    let highs: Vec<usize> = events.iter().enumerate()
        .filter(|(_, e)| e.1).map(|(i, _)| i).collect();
    assert_eq!(highs.len(), 3, "expected 3 HIGH transitions");

    for &hi in &highs {
        if hi + 1 < events.len() {
            assert_duration(&events, hi, 100, &format!("repeat block at event {hi}"));
        }
    }
    Ok(())
}

// ── Test 6: Pause block ───────────────────────────────────────────────────────

#[tokio::test]
async fn test_06_pause_block() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // [version=1, cmd=vibrate, repeat=1, block=short_pause(80ms)]
    h.write_command(&[0x01, 0x01, 0x01, 0x04]).await?;

    assert!(wait_for_idle(&h.queue, Duration::from_secs(2)), "queue did not drain");
    tokio::time::sleep(Duration::from_millis(30)).await;

    // Pause block: motor_on=false → the motor pin should never go HIGH
    let events = h.motor.events();
    let any_high = events.iter().any(|e| e.1);
    assert!(!any_high, "motor went HIGH during a pause-only command");
    Ok(())
}

// ── Test 7: Mixed sequence ────────────────────────────────────────────────────

#[tokio::test]
async fn test_07_mixed_sequence() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // [v=1, cmd=vibrate, repeat=1, short_buzz(100), short_pause(80), long_buzz(500)]
    h.write_command(&[0x01, 0x01, 0x01, 0x01, 0x04, 0x03]).await?;

    assert!(
        wait_for_idle(&h.queue, Duration::from_secs(4)),
        "queue did not drain"
    );
    tokio::time::sleep(Duration::from_millis(30)).await;

    let events = h.motor.events();
    // Expected GPIO pattern: HIGH(100ms) LOW(80ms) HIGH(500ms) LOW
    assert!(events.len() >= 4, "expected ≥4 events for mixed sequence, got {}", events.len());

    // First transition: HIGH for ~100ms
    assert!(events[0].1,  "event[0] should be HIGH");
    assert_duration(&events, 0, 100, "short_buzz");

    // Second transition: LOW for ~80ms  (pause block sets motor_on=false)
    assert!(!events[1].1, "event[1] should be LOW (pause)");
    assert_duration(&events, 1, 80, "short_pause");

    // Third transition: HIGH for ~500ms
    assert!(events[2].1,  "event[2] should be HIGH (long_buzz)");
    assert_duration(&events, 2, 500, "long_buzz");
    Ok(())
}

// ── Test 8: Queue accumulation ────────────────────────────────────────────────

#[tokio::test]
async fn test_08_queue_accumulation() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // Two rapid writes: short_buzz + click
    h.write_command(&[0x01, 0x01, 0x01, 0x01]).await?; // 100 ms
    h.write_command(&[0x01, 0x01, 0x01, 0x07]).await?; // 40 ms

    assert!(
        wait_for_idle(&h.queue, Duration::from_secs(3)),
        "queue did not drain"
    );
    tokio::time::sleep(Duration::from_millis(30)).await;

    let events = h.motor.events();
    // Expect two HIGH pulses (100ms then 40ms) executed in order
    let highs: Vec<usize> = events.iter().enumerate()
        .filter(|(_, e)| e.1).map(|(i, _)| i).collect();
    assert_eq!(highs.len(), 2, "expected 2 HIGH pulses from 2 queued commands");

    assert_duration(&events, highs[0], 100, "first queued block (short_buzz)");
    assert_duration(&events, highs[1], 40,  "second queued block (click)");
    Ok(())
}

// ── Test 9: Unknown block IDs skipped ────────────────────────────────────────

#[tokio::test]
async fn test_09_unknown_block_ids_skipped() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // short_buzz(01) + unknown(FF) + long_buzz(03) → only two buzz blocks execute
    h.write_command(&[0x01, 0x01, 0x01, 0x01, 0xFF, 0x03]).await?;

    assert!(wait_for_idle(&h.queue, Duration::from_secs(3)), "queue did not drain");
    tokio::time::sleep(Duration::from_millis(30)).await;

    let events = h.motor.events();
    let highs: Vec<usize> = events.iter().enumerate()
        .filter(|(_, e)| e.1).map(|(i, _)| i).collect();
    assert_eq!(highs.len(), 2, "expected 2 HIGH pulses (unknown block 0xFF should be skipped)");

    assert_duration(&events, highs[0], 100, "short_buzz");
    assert_duration(&events, highs[1], 500, "long_buzz");
    Ok(())
}

// ── Test 10: Wrong version rejected ──────────────────────────────────────────

#[tokio::test]
async fn test_10_wrong_version_rejected() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // Version 2 — device runs version 1 → should be rejected
    h.write_command(&[0x02, 0x01, 0x01, 0x01]).await.ok(); // may return GATT error

    // Give the vibration thread time to do anything (it should do nothing)
    tokio::time::sleep(Duration::from_millis(200)).await;

    let events = h.motor.events();
    let any_high = events.iter().any(|e| e.1);
    assert!(!any_high, "motor moved despite wrong version byte");

    // Device should still be connected
    assert!(
        h.device.is_connected().await?,
        "device disconnected after version rejection"
    );
    Ok(())
}

// ── Test 11: Unknown command rejected ────────────────────────────────────────

#[tokio::test]
async fn test_11_unknown_command_rejected() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // Command 0x42 is not defined
    h.write_command(&[0x01, 0x42, 0x01, 0x01]).await.ok();

    tokio::time::sleep(Duration::from_millis(200)).await;

    let events = h.motor.events();
    assert!(
        !events.iter().any(|e| e.1),
        "motor moved despite unknown command byte"
    );
    assert!(h.device.is_connected().await?, "device disconnected after command rejection");
    Ok(())
}

// ── Test 12: Disconnect → re-advertise ───────────────────────────────────────

#[tokio::test]
async fn test_12_disconnect_readvertise() -> Result<()> {
    let session = bluer::Session::new().await?;
    let adapter = match session.default_adapter().await {
        Ok(a) => a,
        Err(_) => { eprintln!("SKIP test_12: no adapter"); return Ok(()); }
    };
    adapter.set_powered(true).await?;

    let queue = vibration::new_queue();
    let motor = MockGpio::new();
    let led   = MockGpio::new();
    vibration::run_thread(queue.clone(), motor, led);
    let _peripheral = ble_peripheral::start(&adapter, queue).await?;
    tokio::time::sleep(Duration::from_millis(200)).await;

    // First connection
    let device = connect_central(&adapter).await?;
    assert!(device.is_connected().await?);

    // Disconnect
    device.disconnect().await?;
    tokio::time::sleep(Duration::from_millis(500)).await;

    // The peripheral should be re-advertising; a second connect should work.
    let device2 = connect_central(&adapter).await
        .context("re-advertising and re-connect after disconnect")?;
    assert!(device2.is_connected().await?, "device not connected on second attempt");
    Ok(())
}

// ── Test 13: Mutex not held across sleep ──────────────────────────────────────

#[tokio::test]
async fn test_13_mutex_not_held_across_sleep() -> Result<()> {
    let h = match Harness::new().await {
        Ok(h) => h,
        Err(e) if e.to_string().starts_with("SKIP") => { eprintln!("{}", e); return Ok(()); }
        Err(e) => return Err(e),
    };

    // Enqueue a long buzz (500 ms execution time).
    h.write_command(&[0x01, 0x01, 0x01, 0x03]).await?;

    // Wait 50 ms — vibration is now in progress holding the motor GPIO high.
    tokio::time::sleep(Duration::from_millis(50)).await;

    // Immediately write a second packet. If the mutex is held across the
    // 500 ms sleep, this enqueue would block until the buzz finishes.
    let t0 = Instant::now();
    h.write_command(&[0x01, 0x01, 0x01, 0x07]).await?; // click (40 ms)
    let write_latency = t0.elapsed();

    // Enqueue should return in microseconds; allow up to 250 ms for scheduling jitter.
    assert!(
        write_latency < Duration::from_millis(250),
        "second write blocked for {:?} — mutex may be held across sleep",
        write_latency
    );

    // Both blocks should eventually execute.
    assert!(wait_for_idle(&h.queue, Duration::from_secs(3)), "queue did not drain");
    tokio::time::sleep(Duration::from_millis(30)).await;

    let events = h.motor.events();
    let highs: Vec<usize> = events.iter().enumerate()
        .filter(|(_, e)| e.1).map(|(i, _)| i).collect();
    assert_eq!(highs.len(), 2, "expected 2 HIGH pulses (long_buzz + click), got {}", highs.len());
    Ok(())
}
