use std::collections::VecDeque;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use esp_idf_hal::gpio::{AnyOutputPin, Level, PinDriver};

use crate::protocol::VibBlock;

struct Step {
    motor_on:    bool,
    duration_ms: u64,
}

pub type Queue = Arc<Mutex<VecDeque<Step>>>;

pub fn new_queue() -> Queue {
    Arc::new(Mutex::new(VecDeque::new()))
}

/// Gap inserted between repeat iterations so each one is felt as a distinct
/// pulse instead of blurring into one continuous buzz.
const REPEAT_GAP_MS: u64 = 200;

/// Append `blocks × repeat` steps onto the queue, with a medium pause between
/// each iteration. Safe to call from any thread (BLE callbacks included).
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
pub fn run_thread(queue: Queue, motor_pin: AnyOutputPin, led_pin: AnyOutputPin) {
    thread::Builder::new()
        .name("vibration".into())
        .stack_size(4096)
        .spawn(move || {
            let mut motor = PinDriver::output(motor_pin).expect("motor GPIO");
            let mut led   = PinDriver::output(led_pin).expect("led GPIO");

            loop {
                let next = queue.lock().unwrap().pop_front();
                match next {
                    Some(step) => {
                        let lvl = if step.motor_on { Level::High } else { Level::Low };
                        motor.set_level(lvl).ok();
                        led.set_level(lvl).ok();
                        thread::sleep(Duration::from_millis(step.duration_ms));
                    }
                    None => {
                        motor.set_low().ok();
                        led.set_low().ok();
                        // Poll at 200 Hz while idle; <5 ms latency to first step.
                        thread::sleep(Duration::from_millis(5));
                    }
                }
            }
        })
        .expect("vibration thread spawn");
}
