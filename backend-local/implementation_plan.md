# Backend Local — Implementation Plan

## 1. Vision

A **single Spring Boot codebase** that serves as the local backend for any country.
Country-specific configuration (name, thresholds, tolerances, alert recipients, etc.) is injected at runtime through a **`.env`** file consumed by Docker Compose.
No per-country folder — one image, one compose file, three deployments.

The local backend has **no app-level authentication**. It is a machine-to-machine service, called only by the central backend, and is secured at the network/service layer (private network / VPN / mTLS or a client-credentials token presented by the central backend). User authentication and RBAC live at the edge (frontend + central backend via Keycloak), not here. See §11 for the rationale.
Alert emails are sent through **Odoo's `mail.message` API**, eliminating the need for a standalone SMTP server.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 + Spring Boot 3.x |
| Build | Gradle (Kotlin DSL) or Maven |
| Messaging | Spring Integration MQTT (Eclipse Paho) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Auth | None at app level — M2M service secured at the network/service layer (see §11) |
| Email (alerting) | Odoo `mail.message` API (XML-RPC / JSON-RPC) |
| Resilience | Scheduled tasks (`@Scheduled`) for periodic checks |
| API docs | springdoc-openapi (OpenAPI 3.x / Swagger UI) |
| Containerisation | Dockerfile (multi-stage) + Docker Compose |
| Testing | JUnit 5 + Testcontainers + MockMvc |

---

## 3. Environment Configuration (`.env`)

The `.env` file is the **only** country-specific artefact.
Docker Compose interpolates these variables into Spring's `application.yml` via `${VAR}` placeholders.

```dotenv
# ── Country identity ──────────────────────────────────
COUNTRY_CODE=BR
COUNTRY_NAME=Brésil

# ── Ideal storage conditions ──────────────────────────
TEMPERATURE_IDEALE_C=29
HUMIDITE_IDEALE_POURCENT=55

# ── Tolerances ────────────────────────────────────────
TOLERANCE_TEMPERATURE_C=3
TOLERANCE_HUMIDITE_POURCENT=2

# ── Lot expiry ────────────────────────────────────────
DUREE_MAX_STOCKAGE_JOURS=365

# ── MQTT ──────────────────────────────────────────────
MQTT_BROKER_URL=tcp://mosquitto:1883
MQTT_TOPIC=futurekawa/${COUNTRY_CODE}/entrepot/+/mesures
MQTT_CLIENT_ID=backend-local-${COUNTRY_CODE}
MQTT_QOS=1

# ── PostgreSQL ────────────────────────────────────────
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=futurekawa_${COUNTRY_CODE}
POSTGRES_USER=futurekawa
POSTGRES_PASSWORD=changeme

# ── Odoo (ERP + Email) ────────────────────────────────
ODOO_URL=http://odoo:8069
ODOO_DB=futurekawa
ODOO_API_USER=api-backend@futurekawa.local
ODOO_API_KEY=changeme-odoo-api-key
ALERTE_DESTINATAIRE_EMAIL=responsable.br@futurekawa.local

# ── Server ────────────────────────────────────────────
SERVER_PORT=8081
```

> Three `.env` files will be provided as examples: `.env.bresil`, `.env.equateur`, `.env.colombie`.

---

## 4. Data Model (PostgreSQL)

Following the glossary naming conventions strictly.

### 4.1 SQL Schema (DDL)

Here is the exact PostgreSQL schema, showing explicit Primary Keys, Foreign Keys, unique indexes, and checks to ensure data integrity:

```sql
-- ── PAYS (Country Identity & Global Config) ──────────────────────────────────
CREATE TABLE pays (
    id_pays                     BIGSERIAL PRIMARY KEY,
    code_pays                   VARCHAR(5) UNIQUE NOT NULL, -- e.g., "BR", "EC", "CO"
    nom_pays                    VARCHAR(100) NOT NULL,
    temperature_ideale_c        NUMERIC(4,1) NOT NULL,
    humidite_ideale_pourcent    NUMERIC(4,1) NOT NULL,
    tolerance_temperature_c     NUMERIC(4,1) NOT NULL,
    tolerance_humidite_pourcent NUMERIC(4,1) NOT NULL,
    est_actif                   BOOLEAN NOT NULL DEFAULT TRUE
);

-- ── EXPLOITATION (Farms) ─────────────────────────────────────────────────────
CREATE TABLE exploitation (
    id_exploitation             BIGSERIAL PRIMARY KEY,
    nom_exploitation            VARCHAR(200) NOT NULL,
    localisation                VARCHAR(300),
    responsable_email           VARCHAR(254),
    est_active                  BOOLEAN NOT NULL DEFAULT TRUE,
    pays_id                     BIGINT NOT NULL,
    
    CONSTRAINT fk_exploitation_pays 
        FOREIGN KEY (pays_id) 
        REFERENCES pays(id_pays) 
        ON DELETE RESTRICT
);

-- ── ENTREPOT (Storage Facilities / Warehouses) ───────────────────────────────
CREATE TABLE entrepot (
    id_entrepot                 BIGSERIAL PRIMARY KEY,
    nom_entrepot                VARCHAR(200) NOT NULL,
    localisation                VARCHAR(300),
    capacite_max                INTEGER,
    statut_entrepot             VARCHAR(50) NOT NULL DEFAULT 'actif',
    exploitation_id             BIGINT NOT NULL,
    pays_id                     BIGINT NOT NULL,
    
    CONSTRAINT fk_entrepot_exploitation 
        FOREIGN KEY (exploitation_id) 
        REFERENCES exploitation(id_exploitation) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_entrepot_pays 
        FOREIGN KEY (pays_id) 
        REFERENCES pays(id_pays) 
        ON DELETE RESTRICT
);

-- ── LOT (Coffee Batches) ──────────────────────────────────────────────────────
CREATE TABLE lot (
    id_lot                      BIGSERIAL PRIMARY KEY,
    reference_lot               VARCHAR(100) UNIQUE NOT NULL,
    date_entree_stockage        TIMESTAMP NOT NULL,
    date_recolte                DATE,
    statut_lot                  VARCHAR(20) NOT NULL DEFAULT 'conforme',
    qualite_lot                 VARCHAR(100),
    exploitation_id             BIGINT NOT NULL,
    entrepot_id                 BIGINT NOT NULL,
    pays_id                     BIGINT NOT NULL,
    
    CONSTRAINT chk_statut_lot 
        CHECK (statut_lot IN ('conforme', 'en_alerte', 'perime')),
        
    CONSTRAINT fk_lot_exploitation 
        FOREIGN KEY (exploitation_id) 
        REFERENCES exploitation(id_exploitation) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_lot_entrepot 
        FOREIGN KEY (entrepot_id) 
        REFERENCES entrepot(id_entrepot) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_lot_pays 
        FOREIGN KEY (pays_id) 
        REFERENCES pays(id_pays) 
        ON DELETE RESTRICT
);

-- ── MESURE_STOCKAGE (Time-series Sensor Readings) ─────────────────────────────
CREATE TABLE mesure_stockage (
    id_mesure                   BIGSERIAL PRIMARY KEY,
    date_heure_mesure           TIMESTAMP NOT NULL,
    temperature_c               NUMERIC(5,2) NOT NULL,
    humidite_pourcent           NUMERIC(5,2) NOT NULL,
    source_mesure               VARCHAR(50), -- e.g. "esp32-dht22", "simulator"
    topic_mqtt                  VARCHAR(255),
    entrepot_id                 BIGINT NOT NULL,
    pays_id                     BIGINT NOT NULL,
    
    CONSTRAINT fk_mesure_entrepot 
        FOREIGN KEY (entrepot_id) 
        REFERENCES entrepot(id_entrepot) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_mesure_pays 
        FOREIGN KEY (pays_id) 
        REFERENCES pays(id_pays) 
        ON DELETE RESTRICT
);

-- ── ALERTE (Incidents Log) ───────────────────────────────────────────────────
CREATE TABLE alerte (
    id_alerte                   BIGSERIAL PRIMARY KEY,
    type_alerte                 VARCHAR(30) NOT NULL,
    niveau_alerte               VARCHAR(20) NOT NULL DEFAULT 'warning',
    statut_alerte               VARCHAR(20) NOT NULL DEFAULT 'ouverte',
    message_alerte              TEXT,
    date_creation               TIMESTAMP NOT NULL DEFAULT NOW(),
    date_cloture                TIMESTAMP,
    destinataire_email          VARCHAR(254),
    pays_id                     BIGINT NOT NULL,
    exploitation_id             BIGINT,
    entrepot_id                 BIGINT NOT NULL,
    lot_id                      BIGINT,
    
    CONSTRAINT chk_type_alerte 
        CHECK (type_alerte IN ('condition_non_ideale', 'lot_trop_ancien')),
        
    CONSTRAINT chk_niveau_alerte 
        CHECK (niveau_alerte IN ('info', 'warning', 'critique')),
        
    CONSTRAINT chk_statut_alerte 
        CHECK (statut_alerte IN ('ouverte', 'notifiee', 'cloturee')),
        
    CONSTRAINT fk_alerte_pays 
        FOREIGN KEY (pays_id) 
        REFERENCES pays(id_pays) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_alerte_exploitation 
        FOREIGN KEY (exploitation_id) 
        REFERENCES exploitation(id_exploitation) 
        ON DELETE SET NULL,
        
    CONSTRAINT fk_alerte_entrepot 
        FOREIGN KEY (entrepot_id) 
        REFERENCES entrepot(id_entrepot) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_alerte_lot 
        FOREIGN KEY (lot_id) 
        REFERENCES lot(id_lot) 
        ON DELETE SET NULL
);
```

### 4.2 Indexes

- `lot (date_entree_stockage ASC)` — supports FIFO ordering
- `lot (statut_lot)` — filter by status
- `mesure_stockage (entrepot_id, date_heure_mesure DESC)` — time-series queries
- `alerte (statut_alerte, date_creation DESC)` — open alerts dashboard

### 4.3 Seed Data

On first startup, auto-insert the current country row in `pays` using `.env` values via a `DataInitializer` (`ApplicationRunner`).

---

## 5. MQTT Ingestion

### 5.1 Topic Convention

```
futurekawa/{codePays}/entrepot/{idEntrepot}/mesures
```

Example: `futurekawa/BR/entrepot/3/mesures`

### 5.2 Expected Payload (JSON)

```json
{
  "entrepotId": 3,
  "temperatureC": 30.2,
  "humiditePourcent": 54.8,
  "dateHeureMesure": "2026-06-01T14:32:00Z",
  "idCapteur": "esp32-br-01"
}
```

### 5.3 Processing Pipeline

1. **`MqttInboundAdapter`** (Spring Integration) subscribes to the configured topic.
2. A **`MesureMessageHandler`** service:
   - Deserialises the JSON payload.
   - Validates required fields.
   - Persists a `MesureStockage` entity.
   - Updates `entrepot.temperatureActuelleC` / `humiditeActuellePourcent`.
   - Delegates to the **alerting engine** for threshold evaluation.

---

## 6. REST API

Base path: `/api/v1`

### 6.1 Lots (stock management)

| Method | Path | Description |
|---|---|---|
| `GET` | `/lots` | List lots, ordered by `dateEntreeStockage ASC` (FIFO). Supports query params: `statutLot`, `entrepotId`, `page`, `size`. |
| `GET` | `/lots/{id}` | Detail of a single lot. |
| `POST` | `/lots` | Register a new lot. |
| `PATCH` | `/lots/{id}` | Update lot status. |

### 6.2 Mesures (storage conditions)

| Method | Path | Description |
|---|---|---|
| `GET` | `/entrepots/{id}/mesures` | Time-series of temperature/humidity for an entrepôt. Supports `from`, `to` date range params. |
| `GET` | `/entrepots/{id}/mesures/latest` | Latest reading for an entrepôt. |

### 6.3 Alertes

| Method | Path | Description |
|---|---|---|
| `GET` | `/alertes` | List alerts. Supports `statutAlerte`, `typeAlerte`, `page`, `size`. |
| `GET` | `/alertes/{id}` | Detail of a single alert. |
| `PATCH` | `/alertes/{id}` | Close/acknowledge an alert (set `statutAlerte = cloturee`). |

### 6.4 Entrepôts & Exploitations

| Method | Path | Description |
|---|---|---|
| `GET` | `/exploitations` | List exploitations for this country. |
| `GET` | `/entrepots` | List entrepôts. Supports `exploitationId`. |
| `GET` | `/entrepots/{id}` | Detail with current conditions. |

### 6.5 Country info

| Method | Path | Description |
|---|---|---|
| `GET` | `/pays` | Returns the single country object with thresholds (useful for the central backend). |

### 6.6 Health

| Method | Path | Description |
|---|---|---|
| `GET` | `/actuator/health` | Standard Spring Boot health endpoint (Postgres, MQTT liveness). |

> All endpoints documented with `springdoc-openapi`. Contracts placed in `backend-local/api/openapi.yml` (generated).

---

## 7. Alerting Engine

### 7.1 Condition-based alerts (`condition_non_ideale`)

Triggered **on every incoming MQTT measure** if:

```
|temperatureC - temperatureIdealeC| > toleranceTemperatureC
OR
|humiditePourcent - humiditeIdealePourcent| > toleranceHumiditePourcent
```

Logic:
1. Check thresholds from the `pays` row (loaded at init from `.env`).
2. If breached → create `Alerte` with `statut = ouverte`, `niveau = warning` (or `critique` if deviation > 2× tolerance).
3. Send email via **Odoo `mail.message` API**.
4. Set `statut = notifiee` after successful API response.

### 7.2 Expiry alerts (`lot_trop_ancien`)

Triggered by a **`@Scheduled` cron job** (default: every hour):

```
ancienneteJours = CURRENT_DATE - dateEntreeStockage
IF ancienneteJours > dureeMaxStockageJours (365) AND statutLot != 'perime'
  → set statutLot = 'perime'
  → create Alerte
  → send email via Odoo
```

### 7.3 Email via Odoo API

Instead of a local SMTP server, the backend calls Odoo's `mail.message/create` endpoint via XML-RPC or JSON-RPC:

```
Odoo model: mail.message
Fields:
  subject:    "[FutureKawa] Alerte {typeAlerte} — {nomPays} / {nomEntrepot}"
  body:       HTML body with alert details, current vs. ideal values
  email_from: configured sender (Odoo outgoing mail server)
  partner_ids: resolved from ALERTE_DESTINATAIRE_EMAIL
```

An `OdooEmailService` encapsulates the RPC call, authenticates with `ODOO_API_USER` / `ODOO_API_KEY`, and handles retries.

---

## 8. Project Structure

```
backend-local/
├── implementation_plan.md          ← this file
├── .env.bresil                     ← example env for Brazil
├── .env.equateur                   ← example env for Ecuador
├── .env.colombie                   ← example env for Colombia
├── docker-compose.yml
├── Dockerfile
├── api/
│   └── openapi.yml                 ← generated OpenAPI spec
├── src/
│   └── main/
│       ├── java/com/futurekawa/backendlocal/
│       │   ├── BackendLocalApplication.java
│       │   ├── config/
│       │   │   ├── MqttConfig.java
│       │   │   ├── OdooProperties.java            ← binds Odoo connection vars
│       │   │   ├── PaysProperties.java            ← binds .env thresholds
│       │   │   └── DataInitializer.java            ← seeds pays row
│       │   ├── model/
│       │   │   ├── Pays.java
│       │   │   ├── Exploitation.java
│       │   │   ├── Entrepot.java
│       │   │   ├── Lot.java
│       │   │   ├── MesureStockage.java
│       │   │   ├── Alerte.java
│       │   │   └── enums/
│       │   │       ├── StatutLot.java              ← conforme, en_alerte, perime
│       │   │       ├── StatutAlerte.java            ← ouverte, notifiee, cloturee
│       │   │       ├── NiveauAlerte.java            ← info, warning, critique
│       │   │       └── TypeAlerte.java              ← condition_non_ideale, lot_trop_ancien
│       │   ├── repository/
│       │   │   ├── PaysRepository.java
│       │   │   ├── ExploitationRepository.java
│       │   │   ├── EntrepotRepository.java
│       │   │   ├── LotRepository.java
│       │   │   ├── MesureStockageRepository.java
│       │   │   └── AlerteRepository.java
│       │   ├── service/
│       │   │   ├── LotService.java
│       │   │   ├── MesureService.java
│       │   │   ├── AlerteService.java
│       │   │   ├── OdooEmailService.java           ← sends alerts via Odoo mail.message API
│       │   │   ├── OdooRpcClient.java              ← XML-RPC / JSON-RPC client for Odoo
│       │   │   └── MqttMessageHandler.java
│       │   ├── scheduler/
│       │   │   └── PeremptionScheduler.java
│       │   └── controller/
│       │       ├── LotController.java
│       │       ├── MesureController.java
│       │       ├── AlerteController.java
│       │       ├── EntrepotController.java
│       │       ├── ExploitationController.java
│       │       └── PaysController.java
│       └── resources/
│           ├── application.yml                     ← ${ENV_VAR} placeholders
│           ├── schema.sql                          ← DDL (optional, Hibernate can manage)
│           └── data.sql                            ← optional seed data
├── tests/
│   ├── unit/
│   │   ├── AlerteServiceTest.java
│   │   ├── OdooEmailServiceTest.java
│   │   ├── LotServiceTest.java
│   │   └── MqttMessageHandlerTest.java
│   ├── integration/
│   │   ├── LotControllerIntegrationTest.java
│   │   ├── MesureControllerIntegrationTest.java
│   │   └── MqttIngestionIntegrationTest.java
│   └── fixtures/
│       ├── sample-mesure-payload.json
│       └── sample-lots.sql
└── scripts/
    ├── run-dev.sh
    └── run-tests.sh
```

---

## 9. Docker Compose

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - pg-data:/var/lib/postgresql/data

  mosquitto:
    image: eclipse-mosquitto:2
    ports:
      - "1883:1883"
    volumes:
      - ./infra/mosquitto/mosquitto.conf:/mosquitto/config/mosquitto.conf

  odoo:
    image: odoo:18
    depends_on:
      - odoo-db
    ports:
      - "8069:8069"
    environment:
      HOST: odoo-db
      USER: odoo
      PASSWORD: odoo
    volumes:
      - odoo-data:/var/lib/odoo
      - ./odoo/addons:/mnt/extra-addons     # futurekawa_quality + futurekawa_inventory

  odoo-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: odoo
      POSTGRES_USER: odoo
      POSTGRES_PASSWORD: odoo
    volumes:
      - odoo-db-data:/var/lib/postgresql/data

  backend-local:
    build: .
    env_file: .env
    depends_on:
      - postgres
      - mosquitto
      - odoo
    ports:
      - "${SERVER_PORT:-8081}:${SERVER_PORT:-8081}"

volumes:
  pg-data:
  odoo-data:
  odoo-db-data:
```

> **Note**: Odoo requires its own PostgreSQL instance (`odoo-db`), separate from the backend-local database.

Launching:
```bash
# Copy the desired country config
cp .env.bresil .env
docker compose up --build
```

---

## 10. Implementation Order

| Step | Task | Dependencies |
|---:|---|---|
| 1 | Bootstrap Spring Boot project (Gradle/Maven, dependencies) | — |
| 2 | Define JPA entities + enums following the glossary | — |
| 3 | Create `PaysProperties` + `DataInitializer` for `.env` binding | Step 1 |
| 4 | Implement repositories | Step 2 |
| 5 | Implement `LotService` + `LotController` (CRUD, FIFO ordering) | Step 4 |
| 6 | Implement `MqttConfig` + `MqttMessageHandler` (MQTT ingestion) | Step 4 |
| 7 | Implement `MesureService` + `MesureController` (time-series queries) | Step 6 |
| 8 | Implement `AlerteService` + threshold evaluation logic | Step 6 |
| 9 | Implement `OdooRpcClient` + `OdooEmailService` (email via Odoo API) | Step 8 |
| 10 | Implement `PeremptionScheduler` (lot expiry cron) | Step 8 |
| 12 | Wire up remaining controllers (`Entrepot`, `Exploitation`, `Pays`) | Step 4 |
| 13 | Write `docker-compose.yml` + `Dockerfile` (incl. Odoo service) | Step 1 |
| 14 | Create `.env.bresil`, `.env.equateur`, `.env.colombie` | — |
| 15 | Configure `springdoc-openapi` + export `openapi.yml` | Step 12 |
| 16 | Write unit tests (services, handler, Odoo client mock) | Steps 5–11 |
| 17 | Write integration tests (Testcontainers for Postgres, embedded MQTT, WireMock for Odoo) | Step 16 |
| 18 | End-to-end smoke test via Docker Compose | Step 13 |

---

## 11. Key Design Decisions

1. **Single codebase, `.env`-driven** — avoids code duplication; a country is just a configuration profile.
2. **FIFO is a query concern** — `ORDER BY date_entree_stockage ASC`; no queue data structure needed.
3. **Alert dedup** — do not create a new `condition_non_ideale` alert if an `ouverte` one already exists for the same entrepôt. Only create a new alert once the previous one is `cloturee`.
4. **Odoo as email gateway** — alert emails are sent via Odoo's `mail.message` API rather than a local SMTP server. This centralises email delivery, leverages Odoo's outgoing mail server configuration, and avoids maintaining a separate mail stack.
5. **No app-level auth on the local backend** — the local API is a machine-to-machine service called only by the central backend. Authenticating end users here would be misplaced: user login and RBAC belong at the edge (frontend + central backend via Keycloak). The local API is instead protected at the network/service layer — private network / VPN, and a service credential (mTLS or a client-credentials JWT presented by the central backend) — which is defense-in-depth rather than perimeter-only trust. This keeps Spring Security off the local classpath entirely. MQTT ingestion is likewise internal and trusted at the network level. Defense-in-depth JWT validation can be reintroduced on the local API later if required.
6. **Glossary compliance** — all entity names, field names, enum values, and API paths follow `GLOSSAIRE.md` conventions.
7. **Timezone** — all timestamps stored as UTC; the `.env` may optionally define a display timezone.

---

## 12. Acceptance Criteria

- [ ] `docker compose up` starts all services (Postgres, Mosquitto, Odoo, backend) with zero manual config beyond `.env`.
- [ ] Publishing a valid MQTT payload persists a `mesure_stockage` row.
- [ ] Out-of-range temperature triggers an alert and sends an email via the Odoo `mail.message` API.
- [ ] `GET /api/v1/lots` returns lots sorted by `dateEntreeStockage ASC` (FIFO).
- [ ] A lot older than 365 days is automatically flagged `perime` by the scheduler.
- [ ] REST API endpoints are reachable without authentication (no app-level security on the local backend).
- [ ] Swagger UI is accessible at `/swagger-ui.html`.
- [ ] Switching `.env` from Brazil to Ecuador changes thresholds without code changes.
- [ ] All unit and integration tests pass (`./gradlew test` or `mvn test`).
