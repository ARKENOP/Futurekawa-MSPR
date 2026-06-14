# Mini PC Deployment — FutureKawa Backend Local + IoT Bridge

How to run the **backend-local** microservice and the **Arduino serial→MQTT bridge**
on a mini PC (Ubuntu Server), keeping **PostgreSQL** and **Mosquitto** on the NAS,
and exposing the API publicly through a **Cloudflare Tunnel** running on the NAS —
protected by **Cloudflare Access**.

---

## 1. Target topology

```
                    Internet (colleagues)
                            │  HTTPS
                            ▼
                   ┌─────────────────┐
                   │ Cloudflare edge │  ← TLS + Access (auth)
                   └────────┬────────┘
                            │  encrypted tunnel
        ┌───────────────────▼──────────────────────┐
        │ NAS  (192.168.1.176)                      │
        │   • cloudflared (tunnel)                  │
        │   • PostgreSQL :5432                       │
        │   • Mosquitto  :1883                       │
        └───────────────────┬──────────────────────┘
                            │  LAN (plain HTTP)
        ┌───────────────────▼──────────────────────┐
        │ Mini PC — Ubuntu Server (<MINI_PC_IP>)    │
        │   • Spring Boot backend-local :8081        │
        │   • Python serial bridge ← USB ← Arduino   │
        └───────────────────────────────────────────┘
```

Only the HTTP API is published. PostgreSQL and Mosquitto stay on the LAN.

### Placeholders used below
| Placeholder | Meaning | Example |
|---|---|---|
| `<MINI_PC_IP>` | Mini PC LAN IP (static recommended) | `192.168.1.50` |
| `<NAS_IP>` | NAS LAN IP | `192.168.1.176` |
| `<PUBLIC_HOST>` | Public hostname for the API | `futurekawa-api.example.com` |
| `<APP_DIR>` | Install dir on mini PC | `/opt/futurekawa` |

---

## 2. Prerequisites

**On the mini PC:**
- Ubuntu Server with a **static LAN IP** (or DHCP reservation on the router).
- **Java 25** (matches the build): `sudo apt install openjdk-25-jre-headless` (or use the JDK if you build there).
- **Python 3 + pip** for the bridge.
- The Arduino plugged in via USB.

**On the NAS:**
- PostgreSQL with database `futurekawa` (user `admin`) — already set up.
- Mosquitto reachable on `1883` with `listener 1883 0.0.0.0` + `allow_anonymous true` — already set up.
- `cloudflared` (Cloudflare Tunnel) — installed as part of this guide.
- A domain on Cloudflare (Zero Trust enabled — free tier is fine).

---

## 3. Build the backend JAR

On your dev machine (or the mini PC if it has the JDK + Maven):

```bash
cd backend-local
mvn clean package -Dmaven.test.skip=true
# -> target/backend-local-0.1.0-SNAPSHOT.jar
```

> `-Dmaven.test.skip=true` is required for now: the single test `OpenApiExportTest`
> is not yet ported to Spring Boot 4. Remove the flag once that test is fixed.

Copy the jar to the mini PC:

```bash
ssh user@<MINI_PC_IP> "sudo mkdir -p <APP_DIR> && sudo chown \$USER <APP_DIR>"
scp target/backend-local-0.1.0-SNAPSHOT.jar user@<MINI_PC_IP>:<APP_DIR>/backend-local.jar
```

---

## 4. Configure the backend (`.env` on the mini PC)

Create `<APP_DIR>/.env` on the mini PC (point DB + broker at the NAS):

```dotenv
# Country identity
COUNTRY_CODE=BR
COUNTRY_NAME=Brésil

# Ideal storage conditions
TEMPERATURE_IDEALE_C=29
HUMIDITE_IDEALE_POURCENT=55
TOLERANCE_TEMPERATURE_C=3
TOLERANCE_HUMIDITE_POURCENT=2
DUREE_MAX_STOCKAGE_JOURS=365

# MQTT broker (NAS)
MQTT_BROKER_URL=tcp://<NAS_IP>:1883
MQTT_TOPIC=futurekawa/${COUNTRY_CODE}/entrepot/+/mesures
MQTT_CLIENT_ID=backend-local-${COUNTRY_CODE}
MQTT_QOS=1

# PostgreSQL (NAS)
POSTGRES_HOST=<NAS_IP>
POSTGRES_PORT=5432
POSTGRES_DB=futurekawa
POSTGRES_USER=admin
POSTGRES_PASSWORD=2314

# Server
SERVER_PORT=8081
```

> Lock it down: `chmod 600 <APP_DIR>/.env` (it holds the DB password).

### Required app change: trust proxy headers

Because the API runs behind the Cloudflare tunnel, add this to
`backend-local/src/main/resources/application.yml` (under `server:`) **before building**,
so Swagger UI and redirects use the public hostname instead of `localhost:8081`:

```yaml
server:
  port: ${SERVER_PORT:8081}
  forward-headers-strategy: framework
```

---

## 5. Run the backend as a systemd service

Create `/etc/systemd/system/futurekawa-backend.service`:

```ini
[Unit]
Description=FutureKawa Backend Local (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=futurekawa
WorkingDirectory=/opt/futurekawa
EnvironmentFile=/opt/futurekawa/.env
ExecStart=/usr/bin/java -jar /opt/futurekawa/backend-local.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

> `EnvironmentFile` makes systemd load `.env` into the process environment —
> this is the equivalent of what `run-dev.sh` does for local dev.

Enable + start:

```bash
sudo useradd -r -s /usr/sbin/nologin futurekawa 2>/dev/null || true
sudo chown -R futurekawa:futurekawa /opt/futurekawa
sudo systemctl daemon-reload
sudo systemctl enable --now futurekawa-backend
sudo systemctl status futurekawa-backend
journalctl -u futurekawa-backend -f      # watch logs; expect "Started BackendLocalApplication" + "started bean 'inbound'"
```

Verify locally on the mini PC:

```bash
curl -s http://localhost:8081/api/v1/pays
```

---

## 6. Run the Arduino bridge as a systemd service

### 6.1 Install deps + serial permissions

```bash
sudo apt install python3-pip
pip install --break-system-packages pyserial paho-mqtt   # or use a venv
sudo usermod -aG dialout futurekawa                       # serial port access
```

### 6.2 Find the stable serial path (survives replug)

```bash
ls -l /dev/serial/by-id/
# e.g. usb-Arduino__www.arduino.cc__0043_xxxx-if00 -> ../../ttyACM0
```
Use the `/dev/serial/by-id/...` path instead of `/dev/ttyACM0`.

### 6.3 Copy the bridge

```bash
scp -r iot/serial-bridge user@<MINI_PC_IP>:/opt/futurekawa/
```

### 6.4 Service `/etc/systemd/system/futurekawa-bridge.service`

```ini
[Unit]
Description=FutureKawa Arduino serial -> MQTT bridge
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=futurekawa
ExecStart=/usr/bin/python3 /opt/futurekawa/serial-bridge/serial_mqtt_bridge.py \
  --serial-port /dev/serial/by-id/usb-Arduino__www.arduino.cc__0043_XXXX-if00 \
  --baud 9600 --broker <NAS_IP> --broker-port 1883 \
  --country BR --entrepot-id 1
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

> Only one program may own the serial port. Make sure the Arduino IDE Serial
> Monitor is **closed** — the bridge needs exclusive access.

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now futurekawa-bridge
journalctl -u futurekawa-bridge -f       # expect "-> futurekawa/BR/entrepot/1/mesures {...}" every 5s
```

---

## 7. Cloudflare Tunnel (on the NAS)

If `cloudflared` isn't installed/authenticated yet:

```bash
cloudflared tunnel login
cloudflared tunnel create futurekawa
# note the Tunnel ID + credentials file path it prints
```

Tunnel config (locally-managed example), e.g. `~/.cloudflared/config.yml` on the NAS:

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /root/.cloudflared/<TUNNEL_ID>.json

ingress:
  - hostname: <PUBLIC_HOST>
    service: http://<MINI_PC_IP>:8081     # tunnel on NAS -> API on mini PC over LAN
  - service: http_status:404
```

Route DNS + run:

```bash
cloudflared tunnel route dns futurekawa <PUBLIC_HOST>
cloudflared tunnel run futurekawa          # or install as a service: cloudflared service install
```

> The `service:` target can be any LAN address, not just localhost — that's how the
> tunnel on the NAS reaches the API on the mini PC.

At this point `https://<PUBLIC_HOST>/swagger-ui.html` resolves — **but it is wide open.
Do section 8 before sharing the URL.**

---

## 8. 🚨 Protect it with Cloudflare Access (REQUIRED)

The backend has **no app-level authentication** by design (it was assumed to be on a
private network). Publishing it means anyone could read **and write** lots. Put
**Cloudflare Access (Zero Trust)** in front so auth happens at the edge.

In the **Cloudflare Zero Trust dashboard**:

1. **Access → Applications → Add an application → Self-hosted.**
   - Application domain: `<PUBLIC_HOST>`.

2. **Policy for colleagues (humans):**
   - Action: **Allow**
   - Include: **Emails** = your colleagues' emails (or **Emails ending in** `@yourcompany.com`).
   - They get a Cloudflare login (email OTP / Google) before reaching Swagger UI.

3. **Service token for programmatic / curl / CI access:**
   - **Access → Service Auth → Service Tokens → Create.**
   - Add a second policy on the app: Action **Service Auth**, Include **Service Token** = the one you created.
   - Callers then send:
     ```
     CF-Access-Client-Id: <token-id>.access
     CF-Access-Client-Secret: <token-secret>
     ```
     ```bash
     curl https://<PUBLIC_HOST>/api/v1/pays \
       -H "CF-Access-Client-Id: <id>.access" \
       -H "CF-Access-Client-Secret: <secret>"
     ```

4. **(Optional) Bypass for the health check** so uptime monitors can hit it:
   - Add a policy on path `/actuator/health`, Action **Bypass**, Include **Everyone**.

> Alternatives if you don't want Access: re-add a simple API-key filter / Spring
> Security in the MS, or restrict the tunnel app by IP. Access is the least work.

---

## 9. End-to-end verification

| Check | Command / where | Expected |
|---|---|---|
| Backend up (mini PC) | `journalctl -u futurekawa-backend -f` | `Started BackendLocalApplication`, `started bean 'inbound'` |
| Bridge publishing | `journalctl -u futurekawa-bridge -f` | `-> futurekawa/BR/entrepot/1/mesures {...}` every 5s |
| Ingestion | backend logs | `Saved new mesure for entrepôt 1` |
| Data in DB | `psql` on NAS: `SELECT count(*) FROM mesure_stockage;` | grows over time |
| Local API | `curl localhost:8081/api/v1/entrepots/1/mesures/latest` | latest reading JSON |
| Public API (human) | browser → `https://<PUBLIC_HOST>/swagger-ui.html` | Cloudflare login, then Swagger |
| Public API (token) | `curl` with `CF-Access-*` headers | `200` + JSON |

---

## 10. Troubleshooting

- **Backend can't reach DB/broker** → check `.env` IPs; from the mini PC: `nc -vz <NAS_IP> 5432` and `nc -vz <NAS_IP> 1883`.
- **Bridge: "could not open port"** → Arduino IDE Serial Monitor is open, or user not in `dialout` (re-login after `usermod`).
- **Swagger "Try it out" hits localhost** → `server.forward-headers-strategy: framework` missing; rebuild.
- **Public URL 502** → tunnel can't reach `http://<MINI_PC_IP>:8081`; confirm backend is listening on `0.0.0.0` (default) and the mini PC firewall allows the NAS.
- **Access login loops** → the email policy doesn't include your address, or you're testing an API path with a browser instead of a service token.
- **Measures not saved, `Entrepot not found: 1`** → fresh DB seed didn't run; the entrepôt id in the bridge (`--entrepot-id`) must exist in the DB.

---

## 11. Updating the deployment

```bash
# rebuild jar on dev machine
mvn -C backend-local clean package -Dmaven.test.skip=true
scp backend-local/target/backend-local-0.1.0-SNAPSHOT.jar user@<MINI_PC_IP>:/opt/futurekawa/backend-local.jar
ssh user@<MINI_PC_IP> "sudo systemctl restart futurekawa-backend"
```
