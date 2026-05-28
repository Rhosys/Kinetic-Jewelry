#include "vibration.h"
#include "config.h"
#include <Arduino.h>

VibrationEngine vibEngine;

void VibrationEngine::begin() {
    pinMode(PIN_MOTOR, OUTPUT);
    digitalWrite(PIN_MOTOR, LOW);
    pinMode(PIN_LED, OUTPUT);
    digitalWrite(PIN_LED, LOW);
}

void VibrationEngine::queueBlocks(const uint8_t* blocks, uint8_t blockCount, uint8_t repeat) {
    for (uint8_t r = 0; r < repeat; r++) {
        for (uint8_t i = 0; i < blockCount; i++) {
            uint32_t dur = blockDurationMs(blocks[i]);
            if (dur == 0) continue;
            _steps.push({blockMotorOn(blocks[i]), dur});
        }
    }
    if (!_stepActive) startNextStep();
}

void VibrationEngine::update() {
    if (!_stepActive) return;
    if ((millis() - _stepStart) >= _stepDur) {
        applyMotor(false);
        _stepActive = false;
        startNextStep();
    }
}

bool VibrationEngine::isActive() const {
    return _stepActive || !_steps.empty();
}

void VibrationEngine::startNextStep() {
    if (_steps.empty()) return;
    VibStep s = _steps.front();
    _steps.pop();
    applyMotor(s.motorOn);
    _stepStart  = millis();
    _stepDur    = s.durationMs;
    _stepActive = true;
}

void VibrationEngine::applyMotor(bool on) {
    _motorOn = on;
    digitalWrite(PIN_MOTOR, on ? HIGH : LOW);
    digitalWrite(PIN_LED,   on ? HIGH : LOW);
}

uint32_t VibrationEngine::blockDurationMs(uint8_t blockId) {
    switch (blockId) {
        case BLOCK_SHORT_BUZZ:   return 100;
        case BLOCK_MEDIUM_BUZZ:  return 250;
        case BLOCK_LONG_BUZZ:    return 500;
        case BLOCK_SHORT_PAUSE:  return 80;
        case BLOCK_MEDIUM_PAUSE: return 200;
        case BLOCK_LONG_PAUSE:   return 600;
        case BLOCK_CLICK:        return 40;
        default:                 return 0;
    }
}

bool VibrationEngine::blockMotorOn(uint8_t blockId) {
    return blockId != BLOCK_SHORT_PAUSE &&
           blockId != BLOCK_MEDIUM_PAUSE &&
           blockId != BLOCK_LONG_PAUSE;
}
