use std::collections::VecDeque;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use kinetic_protocol::VibBlock;

use crate::mock_gpio::MockGpio;

pub struct Step {
    pub motor_on:    bool,
    pub duration_ms: u64,
}

pub type Queue = Arc<Mutex<VecDeque<Step>>>;

pub fn new_queue() -> Queue {
    Arc::new(Mutex::new(VecDeque::new()))
}

/// Gap inserted between repeat iterations so each one is felt as a distinct
/// pulse instead of blurring into one continuous buzz.
const REPEAT_GAP_MS: u64 = 200;

/// Append `blocks × repeat` steps onto the queue, with a medium pause between
/// each iteration. Safe to call from any thread (Tokio tasks or BLE write handlers).
pub fn enqueue(queue: &Queue, blocks: &[VibBlock], repeat: u8) {
    let mut q = queue.lock().unwrap();
    for i in 0..repeat {
        if i > 0 {
            q.push_back(Step { motor_on: false, duration_ms: REPEAT_GAP_MS });
        }
        for b in blocks {
            q.push_back(Step { motor_on: b.motor_on, duration_ms: b.duration_ms });
        }
    }
}

/// Spawn the vibration executor thread.
/// Ownership of both GPIO pins transfers into the thread permanently.
/// The mutex is released BEFORE sleeping so BLE callbacks can enqueue at any time.
pub fn run_thread(queue: Queue, motor: MockGpio, led: MockGpio) {
    thread::Builder::new()
        .name("vibration".into())
        .spawn(move || {
            loop {
                let next = queue.lock().unwrap().pop_front(); // lock released here
                match next {
                    Some(step) => {
                        motor.set(step.motor_on);
                        led.set(step.motor_on);
                        thread::sleep(Duration::from_millis(step.duration_ms));
                    }
                    None => {
                        // Poll at 200 Hz while idle — < 5 ms latency to first step.
                        thread::sleep(Duration::from_millis(5));
                    }
                }
            }
        })
        .expect("vibration thread spawn");
}

/// Wait until the queue is empty for two consecutive 20 ms polls.
/// Returns true if the queue drained within `timeout`, false if it timed out.
pub fn wait_for_idle(queue: &Queue, timeout: Duration) -> bool {
    let deadline = std::time::Instant::now() + timeout;
    loop {
        if std::time::Instant::now() > deadline {
            return false;
        }
        if queue.lock().unwrap().is_empty() {
            // Confirm it stays empty for one more poll cycle.
            thread::sleep(Duration::from_millis(20));
            if queue.lock().unwrap().is_empty() {
                return true;
            }
        }
        thread::sleep(Duration::from_millis(10));
    }
}
