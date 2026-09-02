# FutureKawa — IoT Sensor Node

Capteur de température / humidité pour le stockage du café. Un **Arduino Uno**
lit un **DHT22** et imprime une ligne JSON par mesure sur le port série USB.
L'Uno n'ayant pas de carte réseau, un **pont série → MQTT** en Python (tournant
sur le PC/mini-PC hôte) horodate chaque ligne et la publie vers **Mosquitto**,
que le backend Spring Boot consomme.

```
 ┌──────────────┐  DATA (1-wire)  ┌──────────────┐  USB série JSON  ┌───────────────┐   MQTT   ┌────────────┐   JDBC   ┌────────────┐
 │    DHT22     │ ───────────────>│ Arduino Uno  │ ────────────────>│ serial-bridge │ ───────> │ Mosquitto  │ ───────> │ backend +  │
 │ temp/humid.  │   pin D2        │  (firmware)  │   9600 baud      │   (Python)    │  1883    │  (broker)  │          │ PostgreSQL │
 └──────────────┘                 └──────────────┘                  └───────────────┘          └────────────┘          └────────────┘
```

- **Firmware** : [`arduino-uno/futurekawa_dht22.ino`](arduino-uno/futurekawa_dht22.ino)
- **Pont**     : [`serial-bridge/serial_mqtt_bridge.py`](serial-bridge/serial_mqtt_bridge.py)

---

## 1. Matériel

| Composant                | Détail                                      |
|--------------------------|---------------------------------------------|
| Arduino Uno              | Alimenté par l'USB du PC hôte               |
| Capteur DHT22 (AM2302)   | Température (±0,5 °C) + humidité (±2–5 %RH) |
| Câble USB A→B            | Uno ↔ PC (données + alimentation)           |
| (bare sensor) 10 kΩ      | Résistance de tirage DATA→VCC — voir §3     |

---

## 2. Schéma de câblage

Le firmware utilise **D2** comme broche DATA et active le pull-up interne de
l'Uno (`pinMode(DHTPIN, INPUT_PULLUP)`), qui remplace la résistance 10 kΩ pour
un montage court.

```
        Arduino Uno                         DHT22 (module 3 broches)
      ┌───────────────┐                     ┌──────────────────┐
      │           5V  ├─────────────────────┤ + / VCC  (3–5 V) │
      │               │                     │                  │
      │           D2  ├─────────────────────┤ OUT / DATA       │
      │               │                     │                  │
      │           GND ├─────────────────────┤ - / GND          │
      └───────────────┘                     └──────────────────┘
```

### Tableau de raccordement

| DHT22 (broche)        | Arduino Uno | Rôle                          |
|-----------------------|-------------|-------------------------------|
| VCC / `+`             | `5V`        | Alimentation                  |
| DATA / `OUT`          | `D2`        | Ligne de données 1-wire       |
| GND / `-`             | `GND`       | Masse                         |

> **Module 3 broches** (DHT22 sur carte) : la résistance de tirage est déjà
> intégrée → câblage direct comme ci-dessus.
>
> **Capteur nu 4 broches** (DHT22/AM2302 seul) : brancher broche 1→5V,
> broche 2→D2, broche 4→GND (broche 3 non connectée) et **ajouter une
> résistance 10 kΩ entre DATA et VCC**. Pour changer de broche, éditer
> `#define DHTPIN 2` dans le firmware.

---

## 3. Flasher le firmware

1. Ouvrir `arduino-uno/futurekawa_dht22.ino` dans l'IDE Arduino.
2. Installer la bibliothèque **DHT sensor library** (Adafruit) via le
   gestionnaire de bibliothèques.
3. Carte : *Arduino Uno* — sélectionner le port, puis **Téléverser**.
4. Le moniteur série (**9600 baud**) doit afficher une ligne JSON toutes les 5 s :

   ```json
   {"id_capteur":"arduino-uno-br-01","temperature_c":27.70,"humidite_pourcent":46.40}
   ```

   Les lignes préfixées par `#` (bannière, erreurs de lecture) sont ignorées par
   le pont.

> ⚠️ Un seul programme peut ouvrir le port série : **fermer le moniteur série**
> de l'IDE avant de lancer le pont.

---

## 4. Lancer le pont série → MQTT

```bash
cd iot/serial-bridge
pip install -r requirements.txt          # pyserial + paho-mqtt

python serial_mqtt_bridge.py \
    --serial-port /dev/ttyACM0 \          # port de l'Uno (ls /dev/serial/by-id/)
    --baud 9600 \
    --broker <IP_MOSQUITTO> --broker-port 1883 \
    --country BR --entrepot-id 1
```

> `--country` et `--entrepot-id` sont **obligatoires** et n'ont pas de valeur par
> défaut : ils sont le seul lien entre un capteur physique et un entrepôt (le backend
> lit l'entrepôt dans le topic, jamais dans le payload). Avec une valeur par défaut,
> un second pont démarré sans ces options publierait les relevés de son entrepôt sous
> l'identifiant du premier, en silence.

Le pont ajoute `timestamp` (epoch ms) et publie sur :

```
Topic   : futurekawa/{country}/entrepot/{entrepot_id}/mesures
Payload : {"id_capteur":"arduino-uno-br-01","temperature_c":27.70,
           "humidite_pourcent":46.40,"timestamp":1718373120000}
```

Le backend s'abonne à `futurekawa/{COUNTRY_CODE}/entrepot/+/mesures` : l'`entrepot_id`
passé au pont **doit exister** en base (sinon `Entrepot not found`).

---

## 5. Vérification de bout en bout

| Étape          | Où / commande                                   | Attendu                         |
|----------------|-------------------------------------------------|---------------------------------|
| Capteur ↔ Uno  | Moniteur série 9600 baud                        | ligne JSON toutes les 5 s       |
| Uno ↔ Pont     | Sortie du pont                                  | `-> futurekawa/BR/entrepot/1/…` |
| Pont ↔ Broker  | `mosquitto_sub -t 'futurekawa/#' -v`            | messages reçus                  |
| Ingestion      | Logs backend                                    | `Saved new mesure for entrepôt 1` |

### Dépannage rapide

- **`could not open port`** → moniteur série de l'IDE ouvert, ou port erroné.
- **`# read failed - check wiring`** → vérifier VCC/GND/DATA et le n° de broche.
- **Rien côté broker** → mauvaise `--broker` IP, ou Mosquitto non joignable (`1883`).
- **Port instable après rebranchement** → utiliser `/dev/serial/by-id/…` plutôt que `/dev/ttyACM0`.
