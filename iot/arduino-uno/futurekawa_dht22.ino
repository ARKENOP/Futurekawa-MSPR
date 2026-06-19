/*
 * FutureKawa — Arduino Uno + DHT22 sensor node.
 *
 * The Uno has no network hardware, so it does NOT speak MQTT directly.
 * It prints ONE clean JSON line per reading over USB serial; a Python
 * serial->MQTT bridge on the PC adds a timestamp and publishes it to
 * Mosquitto on the topic the Spring Boot backend subscribes to.
 *
 * Serial line emitted (matches MqttMesurePayload, minus timestamp):
 *   {"id_capteur":"arduino-uno-br-01","temperature_c":27.70,"humidite_pourcent":46.40}
 *
 * Non-JSON lines (boot banner, read errors) are prefixed with '#' so the
 * bridge can ignore them safely.
 */
#include "DHT.h"

#define DHTPIN 2            // DATA wire. Must match the physical pin.
#define DHTTYPE DHT22       // Sensor type
#define SENSOR_ID "arduino-uno-br-01"
#define READ_INTERVAL_MS 5000   // one reading every 5 s

DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(9600);
  pinMode(DHTPIN, INPUT_PULLUP);  // internal pull-up (substitute for 10k resistor)
  dht.begin();
  Serial.println("# DHT22 starting...");
}

void loop() {
  delay(READ_INTERVAL_MS);  // DHT22 needs >= 2 s between reads

  float humidity = dht.readHumidity();
  float tempC = dht.readTemperature();

  // Retry once if the first read fails.
  if (isnan(humidity) || isnan(tempC)) {
    delay(2000);
    humidity = dht.readHumidity();
    tempC = dht.readTemperature();
  }

  if (isnan(humidity) || isnan(tempC)) {
    Serial.println("# read failed - check wiring and pin number");
    return;
  }

  // Emit one compact JSON object, terminated by newline.
  Serial.print("{\"id_capteur\":\"");
  Serial.print(SENSOR_ID);
  Serial.print("\",\"temperature_c\":");
  Serial.print(tempC, 2);
  Serial.print(",\"humidite_pourcent\":");
  Serial.print(humidity, 2);
  Serial.println("}");
}
