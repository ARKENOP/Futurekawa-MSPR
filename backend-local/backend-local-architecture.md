# Backend Local Architecture

> State as of 27/08/2026. System-wide architecture and the argued design choices live in
> `docs/ARCHITECTURE.md`; this document covers the internals of this module only.

## Overview
This application serves as the localized backend for a specific country (e.g., Brazil, Ecuador) handling MQTT ingestion from coffee warehouses, data storage, and alerting via Odoo. It is built using Spring Boot 4 (Java 25) and adheres to a strict layered architecture (Controller → Service → Repository).

It is a module of the `futurekawa-parent` Maven reactor and depends on **`futurekawa-lib`**,
the API contract shared with `backend-central`. Build it from the repository root:
`mvn -pl backend-local -am clean package`.

## Layered Architecture and Class Interactions

### 1. Configuration & Bootstrap (`config/`, `scheduler/`, root)
- **`BackendLocalApplication`**: Entry point, annotated with `@SpringBootApplication` and `@EnableScheduling`.
- **`PaysProperties`, `MqttProperties`, `OdooProperties`**: Binds `.env` variables to Java records.
- **`DataInitializer`**: Uses `PaysProperties` to seed the database with the local country if not present on startup.
- **`MqttConfig`**: Configures the Spring Integration inbound channel for Mosquitto using `MqttProperties`.
- **`OpenApiConfig`**: Configures the OpenAPI/Swagger UI metadata.
- **`PeremptionScheduler`**: Runs an hourly `@Scheduled` job to find expired coffee lots, updating their status and triggering alerts.

### 2. Domain Model (`model/`)
JPA Entities mapped to PostgreSQL:
- **`Pays`, `Exploitation`, `Entrepot`**: Represent the hierarchical location data.
- **`Lot`**: Represents a batch of coffee beans stored in an `Entrepot`.
- **`MesureStockage`**: Represents a single temperature/humidity reading.
- **`Alerte`**: Represents an anomaly (expiry or threshold deviation).
- **Enums**: `StatutLot`, `StatutAlerte`, `NiveauAlerte`, `TypeAlerte` strongly type our states. They live in the shared `futurekawa-lib` module (`com.futurekawa.lib.enums`) because backend-central types the same states.

### 3. Data Access Layer (`repository/`)
Spring Data JPA Interfaces:
- **`PaysRepository`, `ExploitationRepository`, `EntrepotRepository`**
- **`LotRepository`**: Implements FIFO ordering and complex queries (e.g., older than threshold date).
- **`MesureStockageRepository`**: Handles time-series queries for metrics.
- **`AlerteRepository`**: Handles deduplication queries to prevent spamming.

### 4. Integration Layer (`mqtt/`, `odoo/`)
- **`MqttMessageHandler`**: Acts as an `@ServiceActivator` to ingest MQTT messages. It parses the JSON payload to `MqttMesurePayload`, extracts the `entrepotId` from the topic, and delegates to `MesureService`.
- **`OdooRpcClient`**: A standalone `RestClient` wrapper to authenticate and execute JSON-RPC against Odoo.
- **`OdooQualityAlertService`**: Uses `OdooRpcClient` to create a `futurekawa.quality.alert` record asynchronously (`@Async`) when an alert is raised, and to write its `state` when the alert is closed. Odoo owns the e-mail notification; this service only pushes tickets.

### 5. Business Logic Layer (`service/`)
- **`PaysService`, `ExploitationService`, `EntrepotService`**: Standard CRUD and aggregation.
- **`LotService`**: Handles lot creation and status updates.
- **`MesureService`**: Saves metrics from MQTT. Contains logic to check readings against `PaysProperties` tolerances, delegating to `AlerteService` if thresholds are breached.
- **`AlerteService`**: Manages alert lifecycle. Implements deduplication (e.g., don't open a new alert if one is already open for the same warehouse). Calls `OdooQualityAlertService` to raise the ERP ticket and to propagate a closure.

### 6. Data Transfer Objects & Mappers (`futurekawa-lib`, `dto/`, `mapper/`)
- **Shared DTOs**: the request/response records that make up the REST contract
  (`CreateLotRequest`, `UpdateLotRequest`, `UpdateAlerteRequest`, `*Response`) live in the
  **`futurekawa-lib`** module (`com.futurekawa.lib.dto`), so backend-central deserializes
  the very same records instead of maintaining a hand-kept mirror.
- **Local-only DTOs**: `dto/MqttMesurePayload` stays here — it is the MQTT wire format, not
  part of the REST contract.
- **Mappers**: MapStruct interfaces (`LotMapper`, `AlerteMapper`, etc.) that automatically generate conversion logic between Entities and DTOs.

### 7. Presentation Layer (`controller/`, `exception/`)
- **Controllers**: `PaysController`, `ExploitationController`, `EntrepotController`, `LotController`, `MesureController`, `AlerteController` handle REST endpoints and return JSON DTOs.
- **`GlobalExceptionHandler`**: An `@RestControllerAdvice` class that intercepts exceptions (e.g., `ResourceNotFoundException`, `@Valid` failures) and formats them into standard RFC 7807 `ProblemDetail` responses.

---

## Remaining Tasks

Phases 10 (security), 11 (Docker/infrastructure), 13 (tests) and 14 (scripts) of the original
implementation plan are **done**: there is no app-level authentication by design, the country
stack ships as `docker-compose.prod.yml` (PostgreSQL + Mosquitto + backend), the suite holds
68 tests behind an 80% JaCoCo gate, and `scripts/run-dev.sh` starts the module locally.

What is left in this module:

- **`createLot` discards the submitted date**: `LotService` overwrites
  `request.dateEntreeStockage()` with `LocalDateTime.now()`, so a backdated lot cannot be
  created — which blocks the FIFO and peremption demos. The entity also stores a
  `LocalDateTime` where the DTO carries a `LocalDate`.
- **List endpoints ignore their filters**: `backend-central` forwards `statutLot`,
  `statutAlerte`, `typeAlerte` and `from`/`to`, but the controllers only accept `Pageable`.
  The repository methods already exist (`findByStatutLot`, `findByStatutAlerte`,
  `findByTypeAlerte`, `findByEntrepotIdAndDateHeureMesureBetween...`) — they are simply not
  wired up.
- **No default FIFO ordering**: `listAll` calls `findAll(pageable)` with no default sort, so
  row order is whatever PostgreSQL returns. The contract promises
  `dateEntreeStockage ASC` for lots and `dateHeureCreation DESC` for alerts.
- **`Alerte.dateHeureCloture` is missing from the entity** although the column exists, so
  `AlerteResponse.dateHeureCloture` is mapped with `ignore = true`.
- **Untested areas**: `OdooRpcClient` (JSON-RPC over HTTP) and entity `equals`/`hashCode`.
- **Alerting relocation**: moving the Odoo integration behind `backend-central` is specified
  in `migration-alerting-to-backend-central-plan.md` and not started.
