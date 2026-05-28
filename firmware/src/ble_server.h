#pragma once
#include <NimBLEDevice.h>

class BleServer {
public:
    void begin();
    void update();   // call from loop() – manages hold/idle timeouts

    bool isConnected() const { return _connected; }

    // Called by NimBLE callbacks defined in ble_server.cpp
    void _onConnect();
    void _onDisconnect();
    void _onPacketReceived(const uint8_t* data, size_t len);

private:
    NimBLEServer* _server         = nullptr;
    bool          _connected      = false;
    bool          _packetReceived = false;
    uint32_t      _connectedAtMs  = 0;
    uint32_t      _lastPacketMs   = 0;
};

extern BleServer bleServer;
