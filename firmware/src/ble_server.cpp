#include "ble_server.h"
#include "vibration.h"
#include "config.h"
#include <Arduino.h>

BleServer bleServer;

// ── NimBLE callback shims ─────────────────────────────────────────────────────

class ServerCB : public NimBLEServerCallbacks {
    void onConnect(NimBLEServer*, ble_gap_conn_desc*) override {
        bleServer._onConnect();
    }
    void onDisconnect(NimBLEServer*) override {
        bleServer._onDisconnect();
    }
};

class CmdCharCB : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* c) override {
        std::string v = c->getValue();
        bleServer._onPacketReceived((const uint8_t*)v.data(), v.size());
    }
};

// ── Public API ────────────────────────────────────────────────────────────────

void BleServer::begin() {
    NimBLEDevice::init(DEVICE_NAME);
    NimBLEDevice::setPower(ESP_PWR_LVL_P3);   // +3 dBm – reasonable for jewelry range

    _server = NimBLEDevice::createServer();
    _server->setCallbacks(new ServerCB());
    _server->advertiseOnDisconnect(true);

    NimBLEService* svc = _server->createService(SERVICE_UUID);

    // Firmware characteristic – READ; returns the protocol version byte so the
    // Android app can filter which blocks to send.
    NimBLECharacteristic* fwChar = svc->createCharacteristic(
        FIRMWARE_CHAR_UUID,
        NIMBLE_PROPERTY::READ
    );
    uint8_t fwVer = FIRMWARE_PROTOCOL_VER;
    fwChar->setValue(&fwVer, 1);

    // Command characteristic – WRITE (with response so Android gets confirmation).
    NimBLECharacteristic* cmdChar = svc->createCharacteristic(
        COMMAND_CHAR_UUID,
        NIMBLE_PROPERTY::WRITE
    );
    cmdChar->setCallbacks(new CmdCharCB());

    svc->start();

    NimBLEAdvertising* adv = NimBLEDevice::getAdvertising();
    adv->addServiceUUID(SERVICE_UUID);
    adv->setScanResponse(true);
    adv->start();

    Serial.println("[BLE] Advertising as \"" DEVICE_NAME "\"");
}

void BleServer::update() {
    if (!_connected) return;

    uint32_t now = millis();

    if (_packetReceived) {
        // Disconnect HOLD_CONNECTED_MS after the last received packet.
        if ((now - _lastPacketMs) >= HOLD_CONNECTED_MS) {
            Serial.println("[BLE] Hold window expired – disconnecting");
            _server->disconnect(0);
        }
    } else {
        // No packet yet: disconnect if idle too long (safety valve).
        if ((now - _connectedAtMs) >= IDLE_TIMEOUT_MS) {
            Serial.println("[BLE] Idle timeout – disconnecting");
            _server->disconnect(0);
        }
    }
}

// ── Internal callbacks ────────────────────────────────────────────────────────

void BleServer::_onConnect() {
    _connected      = true;
    _packetReceived = false;
    _connectedAtMs  = millis();
    Serial.println("[BLE] Connected");
}

void BleServer::_onDisconnect() {
    _connected = false;
    Serial.println("[BLE] Disconnected");
    // NimBLE restarts advertising automatically (advertiseOnDisconnect = true).
}

void BleServer::_onPacketReceived(const uint8_t* data, size_t len) {
    if (len < 3) {
        Serial.printf("[BLE] Packet too short (%u bytes)\n", (unsigned)len);
        return;
    }

    uint8_t version = data[0];
    uint8_t command = data[1];
    uint8_t repeat  = data[2];

    if (version != PROTO_VERSION) {
        Serial.printf("[BLE] Unsupported protocol version %u\n", version);
        return;
    }
    if (command != CMD_VIBRATE) {
        Serial.printf("[BLE] Unknown command 0x%02X\n", command);
        return;
    }
    if (repeat == 0) return;

    const uint8_t* blocks     = data + 3;
    uint8_t        blockCount = (uint8_t)(len - 3);

    Serial.printf("[BLE] Vibrate: repeat=%u blocks=%u\n", repeat, blockCount);
    vibEngine.queueBlocks(blocks, blockCount, repeat);

    _packetReceived = true;
    _lastPacketMs   = millis();
}
