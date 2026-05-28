use std::sync::{Arc, Mutex};
use std::time::Instant;

/// Thread-safe recording of every (timestamp, level) call made on a GPIO pin.
/// Passed into the vibration thread; cloned into test assertions.
#[derive(Clone, Default)]
pub struct MockGpio {
    history: Arc<Mutex<Vec<(Instant, bool)>>>,
}

impl MockGpio {
    pub fn new() -> Self {
        Self { history: Arc::new(Mutex::new(Vec::new())) }
    }

    /// Record a level change.
    pub fn set(&self, on: bool) {
        self.history.lock().unwrap().push((Instant::now(), on));
    }

    /// Return a snapshot of all recorded events (timestamp, level).
    pub fn events(&self) -> Vec<(Instant, bool)> {
        self.history.lock().unwrap().clone()
    }

    /// Discard recorded events — useful between individual test assertions.
    pub fn clear(&self) {
        self.history.lock().unwrap().clear();
    }

    /// Current level: true = high, false = low (or unset).
    pub fn is_high(&self) -> bool {
        self.history.lock().unwrap().last().map(|e| e.1).unwrap_or(false)
    }
}
