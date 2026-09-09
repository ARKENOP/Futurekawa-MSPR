# Mini PC Deployment — FutureKawa demonstration platform

How the whole solution runs on one mini PC: the **siège** (backend-central +
frontend behind nginx), the **Brésil** country backend with its physical Arduino
serial→MQTT bridge, and a second **Équateur** country stack complete with its own
PostgreSQL and Mosquitto. The API is also exposed publicly through a **Cloudflare
Tunnel** on the NAS, protected by **Cloudflare Access**.

> Sections 3 to 11 describe the Brésil backend and its bridge in detail — that is the
> tier with real hardware. Section 12 covers the siège stack and how a further country
> is added.

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
        ┌───────────────────▼──────────────────────────────────┐
        │ NAS  (192.168.1.176)                                  │
        │   • cloudflared (tunnel)                              │
        │   • PostgreSQL :5432   ← Brésil backend + Odoo        │
        │   • Mosquitto  :1883   ← the physical Arduino         │
        │   • Odoo 18    :8069   ← the ERP, shared by all pays  │
        └───────────────────┬──────────────────────────────────┘
                            │  LAN (plain HTTP)
        ┌───────────────────▼──────────────────────────────────┐
        │ Mini PC — Ubuntu Server (<MINI_PC_IP>)                │
        │                                                       │
        │  docker network `siege`                               │
        │   • nginx  :8080  → static frontend + /api/v1 proxy   │
        │   • backend-central :8090                             │
        │   • backend-local (BR) :8081                          │
        │   • backend-local-ec  :8082  (API only)               │
        │                                                       │
        │  docker network `pays-ec`   (private)                  │
        │   • postgres-ec    (not published)                    │
        │   • mosquitto-ec   :1884→1883                         │
        │   • backend-local-ec                                  │
        │                                                       │
        │   • Python serial bridge ← USB ← Arduino Uno (BR)     │
        └───────────────────────────────────────────────────────┘
```

Only HTTP is published. Each country's database and broker sit on that country's own
network: `pays-ec` is unreachable from `siege`, and only Équateur's REST API is joined
to both. Brésil still uses the NAS PostgreSQL and Mosquitto, because its physical
Arduino publishes there.

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

`backend-local` is a module of the `futurekawa-parent` Maven reactor and depends on the
shared `futurekawa-lib` module, so **build from the repository root**, not from inside
`backend-local/`:

```bash
# from the repository root
mvn -pl backend-local -am clean package
# -> backend-local/target/backend-local-0.1.0-SNAPSHOT.jar  (futurekawa-lib is bundled inside)
```

`-pl backend-local -am` builds the country backend plus the library it needs, and skips
`backend-central`. Add `-Dmaven.test.skip=true` to skip the 190 tests when you only need
the artefact; the full `mvn clean verify` also enforces the 80% JaCoCo gate.

Copy the jar to the mini PC:

```bash
ssh user@<MINI_PC_IP> "sudo mkdir -p <APP_DIR> && sudo chown \$USER <APP_DIR>"
scp backend-local/target/backend-local-0.1.0-SNAPSHOT.jar user@<MINI_PC_IP>:<APP_DIR>/backend-local.jar
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
# rebuild the jar on the dev machine, from the repository root
mvn -pl backend-local -am clean package -Dmaven.test.skip=true
scp backend-local/target/backend-local-0.1.0-SNAPSHOT.jar user@<MINI_PC_IP>:/opt/futurekawa/backend-local.jar
ssh user@<MINI_PC_IP> "sudo systemctl restart futurekawa-backend"
```

> Building the image instead of the bare jar (`docker build -f backend-local/Dockerfile.prod .`)
> also has to run from the repository root, for the same reason.

---

## 12. Siège stack and additional countries

Everything on the mini PC is driven by one Compose file, `<APP_DIR>/docker-compose.yml`.

### Layout of `<APP_DIR>`

```
/opt/futurekawa/
├── docker-compose.yml        # siège + every country stack
├── Dockerfile                # backend-local image (prebuilt jar on a JRE)
├── Dockerfile.central        # backend-central image
├── backend-local.jar
├── backend-central.jar
├── .env                      # Brésil  (NAS Postgres/Mosquitto, port 8081)
├── .env.ec                   # Équateur (in-stack Postgres/Mosquitto, port 8082)
├── .env.central              # FUTUREKAWA_LOCALS registry, CORS, timeouts
├── ec/mosquitto.conf         # Équateur broker config
├── web/dist/                 # built frontend (VITE_USE_MOCKS=false)
├── web/nginx.conf            # serves dist + proxies /api/v1 to the central
└── serial-bridge/            # Python venv + bridge script (Brésil)
```

### Why the frontend needs nginx

`frontend-web/src/lib/http.ts` uses `VITE_API_BASE_URL ?? '/api/v1'` — a **relative**
path, which only resolves behind a proxy (in development the Vite proxy plays that
role). `web/nginx.conf` reproduces it in production:

- `location /api/v1/` → `proxy_pass http://backend-central:8090`, so browser and API
  share one origin and CORS never comes into play;
- `try_files $uri $uri/ /index.html`, **mandatory** because the router uses
  `createWebHistory`: without it, reloading `/lots` returns 404.

Build with `VITE_USE_MOCKS=false` and delete `dist/mockServiceWorker.js` — the MSW
service worker belongs to the test recette, not to a deployment.

### Registering countries with the siège

`.env.central` carries the open-ended registry — Docker DNS names, not IP addresses:

```dotenv
FUTUREKAWA_LOCALS=BR=http://backend-local:8081,EC=http://backend-local-ec:8082
```

The central fails fast on a malformed pair or a duplicate code, and logs an error if a
backend reports a `codePays` different from the one it is registered under.

### Adding a country

1. **Create its env file** from an existing one, keeping the shared ERP settings and
   overriding the rest — done on the host so the Odoo API key never leaves it:

   ```bash
   cd /opt/futurekawa
   PWD_NEW=$(openssl rand -hex 20)
   sed -E \
     -e "s|^COUNTRY_CODE=.*|COUNTRY_CODE=CO|" \
     -e "s|^COUNTRY_NAME=.*|COUNTRY_NAME=Colombie|" \
     -e "s|^TEMPERATURE_IDEALE_C=.*|TEMPERATURE_IDEALE_C=26|" \
     -e "s|^HUMIDITE_IDEALE_POURCENT=.*|HUMIDITE_IDEALE_POURCENT=80|" \
     -e "s|^MQTT_BROKER_URL=.*|MQTT_BROKER_URL=tcp://mosquitto-co:1883|" \
     -e "s|^MQTT_TOPIC=.*|MQTT_TOPIC=futurekawa/CO/entrepot/+/mesures|" \
     -e "s|^MQTT_CLIENT_ID=.*|MQTT_CLIENT_ID=backend-local-CO|" \
     -e "s|^POSTGRES_HOST=.*|POSTGRES_HOST=postgres-co|" \
     -e "s|^POSTGRES_USER=.*|POSTGRES_USER=futurekawa|" \
     -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=${PWD_NEW}|" \
     -e "s|^SERVER_PORT=.*|SERVER_PORT=8083|" \
     .env > .env.co && chmod 600 .env.co
   ```

2. **Add its three services** to `docker-compose.yml`, copying the `*-ec` block: a
   `postgres-co` and a `mosquitto-co` on a new private `pays-co` network, and a
   `backend-local-co` on **both** `pays-co` and `siege`. `postgres-co` can share the
   same `env_file`, because the variable names are the ones the postgres image expects.

3. **Register it** by appending `,CO=http://backend-local-co:8083` to
   `FUTUREKAWA_LOCALS` in `.env.central`.

4. `docker compose up -d --build`.

Nothing else changes. The country seeds its own `Pays`, exploitations and entrepôts at
startup (`DataInitializer`, idempotent, names derived from `COUNTRY_NAME`), the central
discovers it on the next sweep, and the frontend picks it up from `GET /api/v1/pays` —
no frontend rebuild, no code edit.

### Publishing measures for a country without hardware

Only Brésil has a sensor. For any other country, publish onto its own broker — the real
ingestion path, so threshold detection, alerting and the Odoo push all run for real:

```bash
docker exec futurekawa-mosquitto-ec sh -c \
  "mosquitto_pub -h localhost -t futurekawa/EC/entrepot/2/mesures -q 1 \
   -m '{\"id_capteur\":\"sim-ec-02\",\"temperature_c\":31.0,\"humidite_pourcent\":65.6,\"timestamp\":1788353279786}'"
```

Name simulated sensors `sim-<pays>-<nn>` so the demonstration data is
self-documenting: `idCapteur` is visible in the API and the UI, which keeps the real
Arduino (`arduino-uno-br-01`) distinguishable from the rest.

> Publishing while a backend is restarting **loses the messages** — QoS 1 without a
> persistent session. Wait for `/actuator/health` before publishing.

### Verification

```bash
curl -s http://<MINI_PC_IP>:8081/actuator/health      # Brésil
curl -s http://<MINI_PC_IP>:8082/actuator/health      # Équateur
curl -s http://<MINI_PC_IP>:8090/api/v1/pays          # both, consolidated
curl -sI http://<MINI_PC_IP>:8080/lots                # SPA fallback -> 200
```

Resilience, worth demonstrating live:

```bash
docker stop futurekawa-backend-local-ec
curl -sD - http://<MINI_PC_IP>:8090/api/v1/lots -o /dev/null | grep -i x-unavailable
#   x-unavailable-countries: EC        -> and the UI shows its outage banner
docker start futurekawa-backend-local-ec
```

> Recreating a country's container briefly opens the central's circuit breaker, so calls
> for that country answer `503` for about 30 seconds before it half-opens. That is the
> breaker doing its job — wait rather than debug.
