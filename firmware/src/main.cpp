#include <Arduino.h>
#include "config.h"
#include "ble_server.h"
#include "vibration.h"

void setup() {
    Serial.begin(115200);
    Serial.printf("[BOOT] KineticJewel firmware protocol v%u\n", FIRMWARE_PROTOCOL_VER);

    vibEngine.begin();
    bleServer.begin();
}

void loop() {
    bleServer.update();
    vibEngine.update();
    delay(1);   // yield 1 ms to the FreeRTOS scheduler / BLE stack
}
