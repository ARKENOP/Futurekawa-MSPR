#!/usr/bin/env python3
"""
FutureKawa — Arduino Uno serial -> MQTT bridge.

The Arduino Uno has no network hardware, so it cannot publish MQTT itself.
This script runs on the PC the Uno is plugged into: it reads the JSON lines
printed over USB serial by futurekawa_dht22.ino, stamps each with the current
time, and republishes them to Mosquitto on the topic the Spring Boot
backend-local subscribes to.

    [Arduino Uno + DHT22] --USB serial JSON--> [this bridge] --MQTT--> [Mosquitto] --> [Spring Boot]

Topic:    futurekawa/{country}/entrepot/{entrepot_id}/mesures
Payload:  {"id_capteur": "...", "temperature_c": 27.70,
           "humidite_pourcent": 46.40, "timestamp": 1718373120000}

Usage:
    pip install pyserial paho-mqtt
    python serial_mqtt_bridge.py \
        --serial-port /dev/ttyACM0 --baud 9600 \
        --broker 192.168.1.176 --broker-port 1883 \
        --country BR --entrepot-id 1
"""
import argparse
import json
import sys
import time

import serial  # pyserial
import paho.mqtt.client as mqtt


def main():
    p = argparse.ArgumentParser(description="Arduino serial -> MQTT bridge")
    p.add_argument("--serial-port", default="/dev/ttyACM0")
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--broker", default="192.168.1.176")
    p.add_argument("--broker-port", type=int, default=1883)
    p.add_argument("--country", default="BR")
    p.add_argument("--entrepot-id", type=int, default=1)
    p.add_argument("--qos", type=int, default=1)
    args = p.parse_args()

    topic = f"futurekawa/{args.country}/entrepot/{args.entrepot_id}/mesures"

    client = mqtt.Client(client_id=f"serial-bridge-{args.country}-{args.entrepot_id}")
    print(f"Connecting to broker {args.broker}:{args.broker_port} ...")
    client.connect(args.broker, args.broker_port, keepalive=60)
    client.loop_start()

    print(f"Opening serial {args.serial_port} @ {args.baud} baud ...")
    with serial.Serial(args.serial_port, args.baud, timeout=5) as ser:
        print(f"Bridging serial -> topic '{topic}'.  Ctrl-C to stop.")
        while True:
            raw = ser.readline()
            if not raw:
                continue
            line = raw.decode("utf-8", errors="replace").strip()
            if not line or line.startswith("#"):
                if line:
                    print(f"  (info) {line}")
                continue

            try:
                payload = json.loads(line)
            except json.JSONDecodeError:
                print(f"  (skip non-JSON) {line}")
                continue

            # Stamp with current wall-clock time (the Uno has no real clock).
            payload["timestamp"] = int(time.time() * 1000)  # epoch milliseconds
            client.publish(topic, json.dumps(payload), qos=args.qos)
            print(f"  -> {topic}  {payload}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped.")
        sys.exit(0)
