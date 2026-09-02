# Architecture applicative — FutureKawa

> État du système au 27/08/2026. Ce document décrit l'architecture **réellement
> implémentée**, pas une cible. Les écarts connus et les évolutions prévues sont
> listés en §7.

## 1. Vue d'ensemble

```mermaid
graph TB
    classDef hq fill:#e3f2fd,stroke:#1565c0,stroke-width:2px;
    classDef local fill:#efebe9,stroke:#4e342e,stroke-width:2px;
    classDef hardware fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef db fill:#fff3e0,stroke:#ef6c00,stroke-width:2px;
    classDef erp fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef shared fill:#ede7f6,stroke:#4527a0,stroke-width:2px;
    classDef docker fill:#e0f7fa,stroke:#00acc1,stroke-width:1px,stroke-dasharray: 5 5;
    classDef devops fill:#fafafa,stroke:#424242,stroke-width:1px,stroke-dasharray: 3 3;

    %% ════════════════════════════════════════════════════
    %% PAYS 1 — BRÉSIL (modèle répliqué pour EC et CO)
    %% ════════════════════════════════════════════════════
    subgraph Pays1 ["🌍 Pays 1 : Brésil (BR) — modèle répliqué à l'identique"]
        direction TB
        Capteur1["🌡️ DHT22<br>température + humidité"]:::hardware
        Uno1["📟 Arduino Uno<br>firmware, JSON sur port série"]:::hardware
        Bridge1["🐍 serial-bridge<br>Python, horodate et publie"]:::hardware

        subgraph DockerLocal1 ["🐳 Docker Compose — stack pays"]
            Broker1["📡 Mosquitto<br>broker MQTT"]:::hardware
            API1["☕ backend-local<br>Spring Boot 4 / Java 25"]:::local
            DB1[("🐘 PostgreSQL 16<br>lots, mesures, alertes")]:::db
        end
    end

    %% ════════════════════════════════════════════════════
    %% PAYS 2 — ÉQUATEUR
    %% ════════════════════════════════════════════════════
    subgraph Pays2 ["🌍 Pays 2 : Équateur (EC)"]
        direction TB
        Capteur2["🌡️ DHT22"]:::hardware
        Uno2["📟 Arduino Uno"]:::hardware
        Bridge2["🐍 serial-bridge"]:::hardware

        subgraph DockerLocal2 ["🐳 Docker Compose — stack pays"]
            Broker2["📡 Mosquitto"]:::hardware
            API2["☕ backend-local"]:::local
            DB2[("🐘 PostgreSQL")]:::db
        end
    end

    %% ════════════════════════════════════════════════════
    %% SIÈGE
    %% ════════════════════════════════════════════════════
    subgraph Siege ["🏢 Siège FutureKawa"]
        direction TB
        subgraph DockerSiege ["🐳 Docker Compose — stack siège"]
            Frontend["🖥️ frontend-web<br>Vue 3 + Pinia + ECharts"]:::hq
            Central["☕ backend-central<br>Spring Boot 4 + Resilience4j"]:::hq
        end
        Odoo["🟣 Odoo 18 Community<br>module futurekawa_quality<br>+ envoi e-mail (SMTP Gmail)"]:::erp
    end

    %% ════════════════════════════════════════════════════
    %% MODULE PARTAGÉ + USINE LOGICIELLE
    %% ════════════════════════════════════════════════════
    Lib["📦 futurekawa-lib<br>contrat d'API partagé<br>DTO + enums métier"]:::shared

    subgraph DevOps ["⚙️ Usine logicielle"]
        direction LR
        Git["Git / GitHub"]:::devops
        CI["GitHub Actions<br>mvn verify + docker build"]:::devops
        Tests["JUnit 5 + JaCoCo<br>Selenium (UI)"]:::devops
        Git --> CI --> Tests
    end

    %% ── Chaîne d'acquisition (interne au pays) ─────────
    Capteur1 -->|"1. 1-wire, broche D2"| Uno1
    Uno1 -->|"2. USB série, JSON 9600 bauds"| Bridge1
    Bridge1 -->|"3. MQTT QoS 1<br>futurekawa/BR/entrepot/{id}/mesures"| Broker1
    Broker1 -->|"4. Spring Integration MQTT"| API1
    API1 -->|"5. Spring Data JPA"| DB1

    Capteur2 --> Uno2 --> Bridge2 --> Broker2 --> API2 --> DB2

    %% ── Consolidation siège ────────────────────────────
    Frontend <-->|"6. HTTP / REST JSON<br>proxy Vite ou CORS"| Central
    Central -.->|"7. REST synchrone + circuit breaker<br>GET /api/v1/**"| API1
    Central -.->|"7. REST synchrone + circuit breaker"| API2

    %% ── Alerting ERP ───────────────────────────────────
    API1 -->|"8. JSON-RPC : création d'une fiche<br>futurekawa.quality.alert"| Odoo
    API2 -->|"8. JSON-RPC"| Odoo
    Odoo -->|"9. e-mail au service qualité<br>(alertes critiques)"| Mail["📧 Responsables qualité"]:::erp

    %% ── Dépendance de build ────────────────────────────
    Lib -.->|"dépendance Maven"| API1
    Lib -.->|"dépendance Maven"| Central

    class Pays1,Pays2 local;
    class Siege hq;
    class DockerLocal1,DockerLocal2,DockerSiege docker;
```

## 2. Composants

| Composant | Technologie | Rôle |
| --- | --- | --- |
| `iot/arduino-uno` | Arduino Uno + DHT22, C++ | Lit température/humidité, imprime une ligne JSON par mesure sur le port série. |
| `iot/serial-bridge` | Python + paho-mqtt | L'Uno n'a pas de carte réseau : le pont lit le port série, horodate, et publie en MQTT. Tourne sur l'hôte du pays. |
| Mosquitto | Eclipse Mosquitto 2 | Broker MQTT local au pays. Ingestion interne, non exposée. |
| `backend-local` | Spring Boot 4.0.6, Java 25 | Un déploiement par pays. Ingestion MQTT, persistance, règles d'alerte, API REST `/api/v1`. |
| PostgreSQL | PostgreSQL 16 | Une base par pays (lots, mesures de stockage, alertes). |
| `backend-central` | Spring Boot 4.0.6 + Resilience4j | Agrège les backends pays, regroupe par `codePays`, expose une API unique au frontend. |
| `frontend-web` | Vue 3, Pinia, Vue Router, ECharts, Vite | Poste de supervision siège : tableau de bord, lots FIFO, courbes, alertes. |
| `futurekawa-lib` | Java, records + enums | **Contrat d'API partagé** entre les deux backends (voir §5). |
| Odoo | Odoo 18 Community, module `futurekawa_quality` | Fiches de non-conformité qualité, workflow d'état, envoi e-mail sur alerte critique. |
| CI | GitHub Actions | `mvn verify` sur le réacteur Maven + build des images Docker + lint/build du frontend. |

## 3. Flux

1. **Acquisition** (interne au pays) : DHT22 → Arduino Uno → pont série Python → Mosquitto → `backend-local` → PostgreSQL. Topic : `futurekawa/{codePays}/entrepot/{idEntrepot}/mesures`, charge utile `{ id_capteur, temperature_c, humidite_pourcent, timestamp }` (epoch millisecondes) — l'identifiant d'entrepôt vient du topic, pas du corps.
2. **Détection** : à chaque mesure, `MesureService` compare la valeur aux seuils du pays (`PaysProperties`, injectés par `.env`). Hors tolérance → `AlerteService` ouvre une alerte `CONDITION_NON_IDEALE` (avec déduplication : pas de seconde alerte ouverte pour le même entrepôt). Un `@Scheduled` horaire lève les alertes `LOT_TROP_ANCIEN` au-delà de 365 jours de stockage.
3. **Notification** : `backend-local` pousse la fiche dans Odoo en JSON-RPC. Odoo est responsable de l'e-mail : le `mail.template` n'est déclenché que pour les alertes de niveau `CRITIQUE`, et les destinataires sont résolus dynamiquement par le tag de contact « Responsable Qualite FutureKawa » (aucune adresse en dur dans le code).
4. **Consolidation** : le frontend n'interroge que `backend-central`, qui fait un **fan-out parallèle** vers les backends pays et enveloppe chaque réponse dans son groupe pays.
5. **Écritures** : création de lot et changement de statut passent par le central, qui relaie vers le bon pays selon `codePays`. Le backend local reste **la source de vérité du cycle de vie des alertes**.
6. **Traitement qualité (bidirectionnel)** : les décisions de l'équipe qualité dans Odoo (*Démarrer l'analyse*, *Valider / Résoudre*, *Déclasser le lot*) sont repoussées vers le backend du pays propriétaire de l'alerte — `PATCH {backend}/api/v1/alertes/{backend_alerte_id}` — l'URL étant lue sur la fiche du pays dans Odoo (*Configuration → Pays*), créée
automatiquement — nom compris — à la première alerte reçue de ce pays. Le backend enregistre le nouveau statut puis le réécrit dans Odoo : cette écriture finale est idempotente, la boucle s'arrête. Sans ce retour, une fiche résolue uniquement dans l'ERP laissait l'alerte `OUVERTE` côté backend, dont la déduplication **supprimait alors toute nouvelle alerte** pour cet entrepôt.

## 4. Choix d'architecture argumentés

### Stabilité

- **Isolation des pannes par pays** : un `CircuitBreaker` Resilience4j par `codePays`. Si un pays ne répond pas (circuit ouvert, timeout, erreur réseau), il est simplement **omis** de la réponse consolidée, qui reste `200 OK` ; les pays absents sont signalés par l'en-tête `X-Unavailable-Countries` et affichés en bandeau par le frontend. Une panne au Brésil ne prive pas le siège des données équatoriennes.
- **Autonomie du pays** : chaque stack pays possède son broker et sa base. La perte du lien avec le siège n'interrompt ni l'acquisition, ni la détection d'alerte, ni l'alimentation d'Odoo.
- **Contrat d'erreur homogène** : les deux backends renvoient du RFC 7807 `application/problem+json`.
- **Redémarrage automatique** : `restart: unless-stopped` et `healthcheck` sur les conteneurs applicatifs.

### Efficacité

- **Fan-out parallèle** sur threads virtuels (Java 25, `newVirtualThreadPerTaskExecutor`) : la latence d'une requête consolidée est celle du pays le plus lent, pas la somme des pays.
- **Timeouts courts** vers les backends pays (2 s connexion / 5 s lecture) pour éviter qu'un pays lent bloque la page.
- **Pagination bornée** : `default-page-size: 20`, `max-page-size: 100`, appliquée dans chaque pays — un paramètre `size` abusif ne peut pas déclencher une lecture non bornée en base.
- **Chargement JPA maîtrisé** : `@EntityGraph` sur les requêtes de liste pour éviter le N+1.

### Pérennité

- **Un seul code source pour N pays** : la configuration pays (seuils, tolérances, identité, connexions) est entièrement externalisée en `.env`. Ouvrir un pays supplémentaire = déployer une stack pays avec son `.env`, ajouter une paire `CODE=url` dans `FUTUREKAWA_LOCALS` côté siège, et renseigner son URL dans Odoo (*Configuration → Pays*, créé automatiquement à la première alerte). **Aucun code pays n'est écrit dans le code** : ni dans les backends, ni dans le module Odoo, ni dans le frontend, qui découvre la liste via `GET /api/v1/pays`.
- **Contrat d'API partagé** (`futurekawa-lib`) : les DTO et enums échangés existent en **un seul exemplaire** compilé, consommé par les deux services. Une divergence de champ devient une erreur de compilation au lieu d'un bug d'intégration.
- **Vocabulaire métier unique** : noms d'entités, de champs, valeurs d'enums et routes suivent `GLOSSAIRE.md`. Convention de langue : **français pour le domaine, anglais pour le code** (commentaires, messages techniques).
- **Découverte des pays** : le central interroge périodiquement `GET /api/v1/pays` de chaque backend pour récupérer nom et seuils — l'ajout d'un pays ne demande pas de redéploiement du frontend.

## 5. Structure de build (réacteur Maven)

```text
pom.xml                 ← futurekawa-parent : agrégateur + configuration commune
├── futurekawa-lib/     ← contrat partagé (DTO + enums), sans Spring ni JPA
├── backend-local/      ← dépend de futurekawa-lib
└── backend-central/    ← dépend de futurekawa-lib
```

`mvn clean verify` à la racine construit les trois modules, exécute les 190 tests de
`backend-local` et applique sa barrière de couverture JaCoCo à 80 %.

**Règle de périmètre de la librairie** : un type ne monte dans `futurekawa-lib` que si
**les deux** services Java l'utilisent. Restent donc hors librairie les enveloppes de
consolidation propres au central (`CountryGroup`, `CountryPageGroup`, `PageDto`), les
entités JPA du local, et `MqttMesurePayload` (format de fil MQTT, pas contrat REST).

Conséquence sur les images Docker : les `Dockerfile.prod` se construisent depuis la
**racine du dépôt** (`context: ..`), car un service seul ne suffit plus à se compiler.

## 6. Sécurité

- **Aucune authentification applicative**, ni sur `backend-local` ni sur `backend-central`.
  Décision assumée pour cette phase : la solution est déployée sur le réseau privé du
  siège et des sites, sans exposition publique. Les backends pays sont des services
  machine-to-machine appelés uniquement par le central.
- La protection repose sur le **niveau réseau/service** : réseau privé, et pour l'accès
  distant de démonstration, un tunnel Cloudflare avec une politique Cloudflare Access
  devant l'API du mini-PC.
- **CORS** est le seul contrôle applicatif côté central (`futurekawa.cors.allowed-origins`),
  nécessaire uniquement quand le frontend est servi depuis une autre origine ; en
  développement le proxy Vite place frontend et API sur la même origine.
- L'écran de connexion du frontend est une **identification de poste** volontairement non
  authentifiante (il nomme l'opérateur pour l'affichage). L'authentification utilisateur et
  le RBAC sont identifiés comme un chantier de la phase suivante.
- Les secrets (mots de passe base, clé d'API Odoo, mot de passe d'application SMTP) vivent
  exclusivement dans des fichiers `.env` non versionnés et dans Odoo.

## 7. Écarts connus et évolutions prévues

| Sujet | État |
| --- | --- |
| Authentification utilisateur / RBAC | Non implémentée (voir §6). Chantier phase 2. |
| Alerting via le central (`local → central → Odoo`) | Non fait, et non nécessaire au fonctionnement : les deux sens passent directement entre le pays et Odoo. Plan détaillé si l'on veut centraliser : `backend-local/migration-alerting-to-backend-central-plan.md`. Le module Odoo adressant le backend par `pays_code` via `ir.config_parameter`, la bascule ne demanderait aucune modification de son code. |
| Odoo `rejected` (« lot déclassé ») | Mappé sur `CLOTUREE`, le backend n'ayant que trois statuts. La nuance « déclassé » reste dans l'état et le journal d'audit de la fiche Odoo ; le `statutLot` du lot n'est pas modifié automatiquement. |
| Module Odoo `futurekawa_inventory` (moteur FIFO) | Non implémenté. Spécifié dans `odoo/implementation_plan.md`. |
| Tests de `backend-central` | 104 tests, ~95 % de lignes, barrière JaCoCo à 80 % active comme sur `backend-local`. Les backends pays y sont substitués au niveau du client (`StubLocalBackendClient`), donc fan-out, disjoncteurs, enveloppes et traduction d'erreurs restent du code de production. |
| Multi-pays simultané sur un même hôte | `docker-compose.prod.yml` fixe les noms de conteneurs et les ports : lancer BR, EC et CO sur la même machine demande de paramétrer projet et ports. |
| Filtrage et pagination côté frontend | Les filtres serveur existent et fonctionnent, mais les vues chargent encore `size=100` par pays puis filtrent et trient en mémoire : au-delà de 100 lots par pays, l'affichage travaille sur un sous-ensemble. |
| Tests de composant frontend | Aucun (ni Vitest ni Vue Test Utils). L'interface est couverte par le lint, le typecheck, le build et la recette Selenium (31 scénarios), pas au niveau du composant isolé. |
| Création d'exploitations / entrepôts | Pas d'API d'écriture : les deux contrôleurs sont en lecture seule, et ces enregistrements sont semés par l'application (`DataInitializer`, idempotent par nom). Conforme au cahier des charges, qui ne demande à l'interface que de *sélectionner* une exploitation (§III.3, §IV.2) ; ajouter un entrepôt demande donc une modification du modèle de semis puis un redéploiement. |
| Liaison capteur ↔ entrepôt | Aucune entité `Capteur` : `idCapteur` n'est qu'une chaîne portée par la mesure, et le seul lien est le topic MQTT. Les brokers étant privés par pays, le risque résiduel est la mauvaise configuration, contenue en rendant `--country` et `--entrepot-id` obligatoires sur le pont série. Une entité `Capteur` reste souhaitable pour la traçabilité (phase 2). |
| Un seul module IoT physique | Le cahier des charges (§III.2, §V.1) demande un module par **pays** et en fournit un par équipe. Un entrepôt est instrumenté pour de vrai (`arduino-uno-br-01`) ; les autres reçoivent des séries simulées publiées sur les mêmes topics du même broker, donc par le même chemin d'ingestion. Les capteurs simulés sont nommés `sim-<pays>-<nn>` pour que la distinction soit visible dans l'API et l'interface. |
| Bus d'événements inter-sites (MQTT bridge) | Documenté comme cible de production, non construit. |
