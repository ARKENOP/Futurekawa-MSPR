#include "DHT.h"

#define DHTPIN 2
#define DHTTYPE DHT22
#define SENSOR_ID "arduino-uno-br-01"
#define READ_INTERVAL_MS 5000

DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(9600);
  pinMode(DHTPIN, INPUT_PULLUP);
  dht.begin();
  Serial.println("# DHT22 starting...");
}

void loop() {
  delay(READ_INTERVAL_MS);

  float humidity = dht.readHumidity();
  float tempC = dht.readTemperature();

  if (isnan(humidity) || isnan(tempC)) {
    delay(2000);
    humidity = dht.readHumidity();
    tempC = dht.readTemperature();
  }

  if (isnan(humidity) || isnan(tempC)) {
    Serial.println("# read failed - check wiring and pin number");
    return;
  }

  Serial.print("{\"id_capteur\":\"");
  Serial.print(SENSOR_ID);
  Serial.print("\",\"temperature_c\":");
  Serial.print(tempC, 2);
  Serial.print(",\"humidite_pourcent\":");
  Serial.print(humidity, 2);
  Serial.println("}");
}
