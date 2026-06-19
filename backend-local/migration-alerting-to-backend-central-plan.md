# Migration Plan — Move Alerting/Odoo Logic to Backend-Central

**Status:** Not started. This document is the roadmap to implement later.

**Goal:** Make `backend-central` the **single integration hub** between the country
`backend-local` instances and Odoo ("Model A" / hub-and-spoke). Central becomes the
*only* component that talks to Odoo.

Flow: `local → central → Odoo` (raise alert) and `Odoo → central → local`
(button actions: Valider / Analyse / Déclasser).

---

## 1. Why this design

Decided 2026-06-19. Alternatives considered:

- **Model A (chosen):** central owns the Odoo connection + credentials; all Odoo
  traffic goes through it.
- **Model B (rejected):** local pushes to Odoo directly; central only routes button
  callbacks. Asymmetric, and the Odoo API key gets copied into every country backend.

**Reasons for Model A:**
- One integration boundary — the Odoo URL/DB/API-key live in **one** place
  (today they would be duplicated in every country's `.env`).
- Symmetric & decoupled — local backends only know *central*; Odoo only knows
  *central*; neither knows the other exists.
- One place for Odoo concerns: auth, retries, the enum↔state mapping, logging.

**Known trade-off:** central becomes critical for the alert→Odoo→email path
(a SPOF). Mitigate with a retry / store-and-forward on the `local → central` hop.
Acceptable for the MSPR; note it in the architecture doc.

**Routing key:** every Odoo ticket already carries `pays_code` + `backend_alerte_id`,
which uniquely identifies any alert across all countries. Central holds a registry
`pays_code → local backend URL`, so it never needs Odoo (or anyone) to know backend
addresses.

---

## 2. Transport decision: HTTP now, MQTT-bridge as the production target

- **Now (MSPR):** plain **HTTP/REST** for the cross-site hops. Simplest — no extra
  infrastructure. Keep MQTT only where it shines (sensors → local backend).
- **Production target (document, don't build):** event-driven over a broker.
  Each country keeps its **edge** Mosquitto for sensors; add **one central broker**
  at HQ; bridge only the alert/command topics up/down
  (`futurekawa/{pays}/alertes/events` out, `.../alertes/commands` in). Gives offline
  queueing (broker holds events if HQ is down) and full decoupling, at the cost of a
  central broker + bridge config per site. This is the standard multi-site IoT pattern.

---

## 3. What moves / what's new

**Move OUT of `backend-local` → INTO `backend-central`:**
- `OdooRpcClient`, `OdooQualityAlertService` (create ticket + update status)
- `OdooProperties` and the `ODOO_*` env vars (so the Odoo API key lives only in central)

**`backend-local` changes:**
- Remove the Odoo client/dependency.
- Add a `CentralClient` (`RestClient`): on alert **raise** and **close**, call central
  instead of Odoo. Local no longer knows Odoo exists.
- New config: `FUTUREKAWA_CENTRAL_URL`, `FUTUREKAWA_INTERNAL_TOKEN`.

**`backend-central` (new Spring Boot app):**
- The moved Odoo gateway.
- `LocalBackendRegistry` — config map `pays_code → local backend base URL`.
- `LocalBackendClient` (`RestClient`) — calls a country's `PATCH /api/v1/alertes/{id}`.
- `InternalAlertController` — receives `local → central` events.
- `OdooWebhookController` — receives Odoo button callbacks.

---

## 4. HTTP contracts

| # | Direction | Endpoint (on central unless noted) | Body |
|---|---|---|---|
| 1 | local → central (raise) | `POST /api/v1/internal/alertes` | `{paysCode, backendAlerteId, entrepotNom, niveau, typeAlerte, lotReference, description, dateCreation}` |
| 2 | local → central (local close) | `POST /api/v1/internal/alertes/{pays}/{id}/status` | `{statut}` |
| 3 | Odoo → central (button) | `POST /api/v1/webhooks/odoo/alertes/status` | `{paysCode, backendAlerteId, odooState}` |
| 4 | central → local (apply) | reuse local `PATCH /api/v1/alertes/{id}` | `{statutAlerte}` |

- #1 / #2 just delegate to the moved `OdooQualityAlertService` (create / update).
- #3 maps `odooState → StatutAlerte`, resolves the backend via the registry, calls #4.
- **Auth:** shared secret header `X-Internal-Token` on the local→central and
  Odoo→central hops for now. Keycloak later for the user-facing `frontend → central`.

### Status mapping

| Local `StatutAlerte` | Odoo `state` |
|---|---|
| `OUVERTE` | `draft` |
| `NOTIFIEE` | `investigation` |
| `CLOTUREE` | `resolved` (or `rejected` if the lot was declassed) |

---

## 5. Odoo module changes (`futurekawa_quality`)

- The 3 button methods (`action_investigate` / `action_approve` / `action_reject`):
  after writing `state`, **POST to central** (contract #3) via Python `requests`,
  reading central URL + token from `ir.config_parameter`
  (`futurekawa.central_url`, `futurekawa.central_token`).
- **`create()` must NOT call central** — the ticket *came from* central; only
  user-clicked buttons call back. This prevents a feedback loop.
- Email stays Odoo-side (the existing `mail.template` fired by `create()`); it does
  not move to central.

### Loop note
Button → central → local PATCH → local notifies central (#2) → central writes Odoo
state. The final write is **idempotent** (same value; an Odoo `write` doesn't fire
button actions), so it terminates — one redundant hop. Optimise later with an
"origin" flag if desired.

---

## 6. Config / deployment

**Central config (example):**
```
futurekawa.odoo.url / db / api-user / api-key        # moved from local
futurekawa.backends.BR=http://<mini-pc>:8081
futurekawa.backends.EC=...
futurekawa.backends.CO=...
futurekawa.internal.token=<shared-secret>
```

**Deployment:** Docker container on the **NAS** (HQ role), co-located with Odoo;
reachable from the mini PC (local backend) over the LAN. Decision pending final
confirmation.

---

## 7. Build order

1. Scaffold `backend-central` (Spring Boot, `pom.xml`, main class, config).
2. Move the Odoo gateway in; add `InternalAlertController`, `OdooWebhookController`,
   `LocalBackendRegistry`, `LocalBackendClient`.
3. Rewire `backend-local`: drop Odoo client, add `CentralClient`, call central on
   raise/close.
4. Odoo buttons → POST to central; add the `ir.config_parameter` entries.
5. Deploy central + rewire local `.env` (remove `ODOO_*`, add `CENTRAL_URL`/token),
   redeploy local, upgrade Odoo module. Test **both** directions end-to-end.

**Cutover risk:** this temporarily disrupts the currently-working `local → Odoo`
alert path (logic is relocating). Do it as one coordinated change and re-verify the
full pipeline afterward.

---

## 8. Current working state (pre-migration, for reference)

As of 2026-06-19, the alert→Odoo→email pipeline already works directly from
`backend-local`: the mini-PC backend pushes tickets to Odoo
(`OdooQualityAlertService`), Odoo emails the quality team on **critical** alerts via
the `mail.template` "Alerte Qualite FutureKawa", and a local close propagates to Odoo
one-way (`OUVERTE→draft`, `NOTIFIEE→investigation`, `CLOTUREE→resolved`). This plan
relocates that Odoo logic into `backend-central`. See the `project_futurekawa` memory
and the `futurekawa_quality` Odoo module for the current implementation.
