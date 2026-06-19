# Backend Local Architecture

## Overview
This application serves as the localized backend for a specific country (e.g., Brazil, Ecuador) handling MQTT ingestion from coffee warehouses, data storage, and alerting via Odoo. It is built using Spring Boot 4 and adheres to a strict layered architecture (Controller → Service → Repository).

## Layered Architecture and Class Interactions

### 1. Configuration & Bootstrap (`config/`, `scheduler/`, root)
- **`BackendLocalApplication`**: Entry point, annotated with `@SpringBootApplication` and `@EnableScheduling`.
- **`PaysProperties`, `MqttProperties`, `OdooProperties`**: Binds `.env` variables to Java records.
- **`DataInitializer`**: Uses `PaysProperties` to seed the database with the local country if not present on startup.
- **`MqttConfig`**: Configures the Spring Integration inbound channel for Mosquitto using `MqttProperties`.
- **`OpenApiConfig`**: Configures the OpenAPI/Swagger UI metadata.
- **`PeremptionScheduler`**: Runs an hourly `@Scheduled` job to find expired coffee lots, updating their status and triggering alerts.

### 2. Domain Model (`model/` & `model/enums/`)
JPA Entities mapped to PostgreSQL:
- **`Pays`, `Exploitation`, `Entrepot`**: Represent the hierarchical location data.
- **`Lot`**: Represents a batch of coffee beans stored in an `Entrepot`.
- **`MesureStockage`**: Represents a single temperature/humidity reading.
- **`Alerte`**: Represents an anomaly (expiry or threshold deviation).
- **Enums**: `StatutLot`, `StatutAlerte`, `NiveauAlerte`, `TypeAlerte` strongly type our states.

### 3. Data Access Layer (`repository/`)
Spring Data JPA Interfaces:
- **`PaysRepository`, `ExploitationRepository`, `EntrepotRepository`**
- **`LotRepository`**: Implements FIFO ordering and complex queries (e.g., older than threshold date).
- **`MesureStockageRepository`**: Handles time-series queries for metrics.
- **`AlerteRepository`**: Handles deduplication queries to prevent spamming.

### 4. Integration Layer (`mqtt/`, `odoo/`)
- **`MqttMessageHandler`**: Acts as an `@ServiceActivator` to ingest MQTT messages. It parses the JSON payload to `MqttMesurePayload`, extracts the `entrepotId` from the topic, and delegates to `MesureService`.
- **`OdooRpcClient`**: A standalone `RestClient` wrapper to authenticate and execute XML-RPC/JSON-RPC against Odoo.
- **`OdooEmailService`**: Uses `OdooRpcClient` to trigger `mail.message/create` asynchronously (`@Async`) for alerting.

### 5. Business Logic Layer (`service/`)
- **`PaysService`, `ExploitationService`, `EntrepotService`**: Standard CRUD and aggregation.
- **`LotService`**: Handles lot creation and status updates.
- **`MesureService`**: Saves metrics from MQTT. Contains logic to check readings against `PaysProperties` tolerances, delegating to `AlerteService` if thresholds are breached.
- **`AlerteService`**: Manages alert lifecycle. Implements deduplication (e.g., don't open a new alert if one is already open for the same warehouse). Calls `OdooEmailService` to notify the manager.

### 6. Data Transfer Objects & Mappers (`dto/`, `mapper/`)
- **DTOs**: Java records used for request/response bodies (e.g., `CreateLotRequest`, `LotResponse`, `MqttMesurePayload`).
- **Mappers**: MapStruct interfaces (`LotMapper`, `AlerteMapper`, etc.) that automatically generate conversion logic between Entities and DTOs.

### 7. Presentation Layer (`controller/`, `exception/`)
- **Controllers**: `PaysController`, `ExploitationController`, `EntrepotController`, `LotController`, `MesureController`, `AlerteController` handle REST endpoints and return JSON DTOs.
- **`GlobalExceptionHandler`**: An `@RestControllerAdvice` class that intercepts exceptions (e.g., `ResourceNotFoundException`, `@Valid` failures) and formats them into standard RFC 7807 `ProblemDetail` responses.

---

## Remaining Tasks

According to our implementation plan (`task.md`), the following phases and tasks are remaining:

### Phase 10 — Security
- **No app-level authentication.** The local backend is a machine-to-machine service (called only by the central backend) with no Spring Security on the classpath. It is secured at the network/service layer (private network / VPN / mTLS or a client-credentials token presented by the central backend). User authentication and RBAC live at the edge (frontend + central backend via Keycloak). MQTT ingestion is internal and trusted at the network level.

### Phase 11 — Docker & Infrastructure
- `Dockerfile` — multi-stage build (Maven build → JRE runtime)
- `docker-compose.yml` — PostgreSQL, Mosquitto, Odoo + odoo-db, backend-local (env_file: .env)
- Mosquitto config (`mosquitto.conf`)
- Create `.env.bresil`, `.env.equateur`, `.env.colombie` from `.env.example`

### Phase 13 — Tests
- **Unit Tests**: `LotServiceTest`, `AlerteServiceTest`, `MqttMessageHandlerTest`, `OdooEmailServiceTest`, `PeremptionSchedulerTest`
- **Integration Tests (Testcontainers)**: `LotControllerIntegrationTest`, `MesureControllerIntegrationTest`, `MqttIngestionIntegrationTest`
- **Fixtures**: `sample-mesure-payload.json`, `sample-lots.sql`

### Phase 14 — Scripts & Final Polish
- `scripts/run-dev.sh` — local dev startup
- `scripts/run-tests.sh` — run all tests
- Final review: glossary compliance, naming conventions, code quality
