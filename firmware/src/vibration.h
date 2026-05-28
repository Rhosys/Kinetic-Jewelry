#pragma once
#include <stdint.h>
#include <queue>

struct VibStep {
    bool     motorOn;
    uint32_t durationMs;
};

class VibrationEngine {
public:
    void begin();

    // Called from BLE callback: enqueue blocks × repeat, then start if idle.
    void queueBlocks(const uint8_t* blocks, uint8_t blockCount, uint8_t repeat);

    // Call every iteration of loop() – advances the step state machine.
    void update();

    bool isActive() const;

private:
    std::queue<VibStep> _steps;
    bool     _stepActive = false;
    bool     _motorOn    = false;
    uint32_t _stepStart  = 0;
    uint32_t _stepDur    = 0;

    static uint32_t blockDurationMs(uint8_t blockId);
    static bool     blockMotorOn(uint8_t blockId);

    void applyMotor(bool on);
    void startNextStep();
};

extern VibrationEngine vibEngine;
