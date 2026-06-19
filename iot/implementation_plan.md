# IoT Module — Implementation Plan

## 1. Vision

A standalone **ESP32 + DHT22** prototype that reads temperature and humidity at a configurable interval, then publishes JSON payloads to the local **Eclipse Mosquitto** MQTT broker over Wi-Fi.
The module must be robust enough for a live soutenance demo and serve as the data source for the entire backend-local pipeline.

---

## 2. Hardware Bill of Materials

| Component | Reference | Role |
|---|---|---|
| Microcontroller | ESP32 DevKit v1 (or WROOM-32) | Wi-Fi connectivity + GPIO |
| Sensor | DHT22 (AM2302) | Temperature (±0.5 °C) + Humidity (±2%) |
| Resistor | 10 kΩ pull-up | Required between DHT22 data pin and VCC |
| Breadboard | Standard 830 points | Prototyping |
| Cables | Dupont M-M / M-F | Wiring |
| USB cable | Micro-USB or USB-C (depending on board) | Power + flashing |

---

## 3. Wiring Diagram

```
ESP32                     DHT22
─────                     ─────
3V3  ──────────────────── VCC (pin 1)
                │
              [10kΩ]
                │
GPIO 4 ────────┴──────── DATA (pin 2)

GND  ──────────────────── GND (pin 4)

(pin 3 of DHT22 is not connected)
```

> GPIO 4 is the default; configurable in the firmware.

---

## 4. Firmware Approach

Two options are available. Choose based on team familiarity:

| Criteria | Arduino (C++) | MicroPython |
|---|---|---|
| Ecosystem | Mature, many libraries | Simpler syntax, rapid prototyping |
| MQTT library | PubSubClient / AsyncMqttClient | umqtt.simple / umqtt.robust |
| DHT library | DHT sensor library (Adafruit) | Built-in `dht` module |
| Recommended for | Production-like feel | Quick iteration |

> **Recommendation**: Arduino (C++) with PlatformIO for a professional setup (dependency management, build system, serial monitor).

---

## 5. Firmware Logic

### 5.1 Boot Sequence

```
1. Initialize serial (115200 baud) for debug logging
2. Initialize DHT22 sensor on GPIO 4
3. Connect to Wi-Fi (SSID / password from config)
4. Connect to MQTT broker (host / port / clientId from config)
5. Enter main loop
```

### 5.2 Main Loop

```
EVERY <INTERVAL_SECONDS> (default: 30s):
  1. Read temperature and humidity from DHT22
  2. If read fails → log error, skip this cycle, retry next
  3. Build JSON payload
  4. Publish to MQTT topic with QoS 1
  5. Log success to serial
```

### 5.3 Reconnection Strategy

- **Wi-Fi**: If disconnected, attempt reconnection every 5 seconds (max 20 retries before reboot).
- **MQTT**: If disconnected, attempt reconnection every 5 seconds with exponential backoff (5s → 10s → 20s → 30s max).
- **Hard reboot**: If both Wi-Fi and MQTT fail for > 5 minutes, trigger `ESP.restart()`.

### 5.4 Configuration Constants

Defined at the top of the firmware file (or in a `config.h`):

```cpp
// ── Wi-Fi ────────────────────────────
#define WIFI_SSID         "FutureKawa-Local"
#define WIFI_PASSWORD     "changeme"

// ── MQTT ─────────────────────────────
#define MQTT_BROKER       "192.168.1.100"    // IP of the Mosquitto container host
#define MQTT_PORT         1883
#define MQTT_CLIENT_ID    "esp32-br-01"
#define MQTT_TOPIC        "futurekawa/BR/entrepot/1/mesures"
#define MQTT_QOS          1

// ── Sensor ───────────────────────────
#define DHT_PIN           4
#define DHT_TYPE          DHT22
#define READ_INTERVAL_MS  30000              // 30 seconds
```

---

## 6. MQTT Topic Convention

Aligned with backend-local expectations:

```
futurekawa/{codePays}/entrepot/{idEntrepot}/mesures
```

Examples:
- `futurekawa/BR/entrepot/1/mesures`
- `futurekawa/EC/entrepot/2/mesures`
- `futurekawa/CO/entrepot/1/mesures`

---

## 7. Payload Format (JSON)

```json
{
  "entrepotId": 1,
  "temperatureC": 29.5,
  "humiditePourcent": 56.2,
  "dateHeureMesure": "2026-06-01T14:32:00Z",
  "idCapteur": "esp32-br-01"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `entrepotId` | integer | yes | Matches the entrepôt in the backend DB |
| `temperatureC` | float | yes | Celsius, 1 decimal |
| `humiditePourcent` | float | yes | Percentage, 1 decimal |
| `dateHeureMesure` | string (ISO 8601) | yes | UTC timestamp from NTP sync |
| `idCapteur` | string | yes | Unique identifier of this sensor unit |

> **NTP sync**: The ESP32 synchronises time via NTP on boot (`pool.ntp.org`) so timestamps are accurate.

---

## 8. Simulator (for development without hardware)

A Python script that emulates the ESP32 by publishing MQTT payloads with randomised-but-realistic values.

### 8.1 Purpose

- Backend developers can work without physical hardware.
- CI tests can validate the MQTT → backend pipeline.
- Demo fallback if hardware fails during soutenance.

### 8.2 Script: `iot/simulator/mqtt_simulator.py`

```python
"""
FutureKawa IoT Simulator
Publishes fake temperature/humidity readings to the local MQTT broker.

Usage:
  pip install paho-mqtt
  python mqtt_simulator.py --broker localhost --country BR --entrepot-id 1
"""
```

Features:
- Configurable via CLI args (`--broker`, `--port`, `--country`, `--entrepot-id`, `--interval`).
- Generates values around the country's ideal ± small random drift.
- Occasionally injects out-of-range readings to trigger alerts (configurable with `--inject-anomalies`).

---

## 9. Project Structure

```
iot/
├── implementation_plan.md          ← this file
├── esp32/
│   ├── firmware/
│   │   ├── platformio.ini          ← PlatformIO project config
│   │   ├── src/
│   │   │   ├── main.cpp            ← main firmware
│   │   │   └── config.h            ← Wi-Fi, MQTT, sensor constants
│   │   ├── lib/
│   │   │   └── README.md           ← (PlatformIO convention)
│   │   └── test/
│   │       └── README.md
│   ├── schemas/
│   │   ├── wiring-diagram.md       ← ASCII or image of the circuit
│   │   └── payload-schema.json     ← JSON Schema for the MQTT payload
│   └── README.md                   ← Setup guide, flashing instructions, troubleshooting
├── simulator/
│   ├── mqtt_simulator.py           ← Python MQTT simulator
│   ├── requirements.txt            ← paho-mqtt
│   └── README.md                   ← Usage instructions
└── payloads-mqtt/
    ├── sample-mesure-normal.json    ← example normal reading
    ├── sample-mesure-anomaly.json   ← example out-of-range reading
    └── payload-schema.json         ← JSON Schema (symlink or copy)
```

---

## 10. Implementation Order

| Step | Task | Dependencies |
|---:|---|---|
| 1 | Set up PlatformIO project with ESP32 board definition | — |
| 2 | Implement Wi-Fi connection with retry logic | Step 1 |
| 3 | Implement NTP time sync | Step 2 |
| 4 | Implement DHT22 sensor reading | Step 1 |
| 5 | Implement MQTT client connection with reconnection | Step 2 |
| 6 | Build JSON payload and publish on timer | Steps 3, 4, 5 |
| 7 | Add serial debug logging | Step 6 |
| 8 | Flash and test with real Mosquitto broker | Step 6 |
| 9 | Write Python simulator script | — |
| 10 | Define JSON Schema for payload validation | — |
| 11 | Create wiring diagram and README | — |
| 12 | Integration test: simulator → Mosquitto → backend-local | Steps 9 + backend |

---

## 11. Key Design Decisions

1. **QoS 1** — ensures at-least-once delivery. The backend handles potential duplicates via idempotent inserts (or simply accepts near-duplicate time-series data).
2. **30-second interval** — sufficient for demo without flooding the broker. Production could be adjusted.
3. **NTP-based timestamps** — the ESP32 clock drifts; NTP sync at boot provides accurate UTC time.
4. **Simulator as first-class citizen** — not an afterthought. It's the primary testing tool and demo fallback.
5. **`config.h` over OTA config** — for a prototype, compile-time constants are simpler and more reliable. OTA configuration is a Phase 2 concern.

---

## 12. Testing Strategy

### 12.1 Hardware Validation

- Verify DHT22 readings against a reference thermometer/hygrometer.
- Monitor serial output to confirm correct JSON formatting.
- Use `mosquitto_sub` on the host to verify messages arrive on the expected topic.

### 12.2 Simulator Validation

- Run simulator against a local Mosquitto container.
- Verify messages with `mosquitto_sub -t "futurekawa/#" -v`.
- Validate payloads against `payload-schema.json`.

### 12.3 End-to-End Validation

- Start full `docker compose up` (backend-local stack).
- Run simulator with `--inject-anomalies`.
- Verify in PostgreSQL that `mesure_stockage` rows appear.
- Verify that `alerte` rows are created for out-of-range readings.
- Check Odoo for alert emails sent via the `mail.message` API.

---

## 13. Acceptance Criteria

- [ ] ESP32 connects to Wi-Fi and MQTT broker within 10 seconds of boot.
- [ ] DHT22 readings are published every 30 seconds on the correct MQTT topic.
- [ ] Payload matches the documented JSON schema.
- [ ] Timestamps are accurate (NTP-synced, UTC).
- [ ] Reconnection works after Wi-Fi or MQTT broker interruption.
- [ ] Python simulator produces realistic readings that are consumed by the backend-local.
- [ ] Simulator's `--inject-anomalies` flag reliably triggers backend alerts.
- [ ] Wiring diagram and README are clear enough for a team member to reproduce the setup.
