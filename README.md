# FutureKawa MSPR

Supervision du stockage de café pour une entreprise multi-pays : capteurs IoT dans les
entrepôts, un backend par pays, une consolidation au siège, et des fiches de
non-conformité dans l'ERP Odoo.

- Architecture et choix techniques argumentés : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- Vocabulaire métier de référence : [`docs/GLOSSAIRE.md`](docs/GLOSSAIRE.md)
- Contrat d'API du siège : [`backend-central/api-contract.md`](backend-central/api-contract.md)
- Stratégie et exécution des tests : [`docs/plan-de-tests.md`](docs/plan-de-tests.md)
- Règles, seuils et notifications d'alerte : [`docs/alerting.md`](docs/alerting.md)

## Arborescence du projet

```text
Futurekawa-MSPR/
├── README.md
├── pom.xml                          # agrégateur Maven : futurekawa-parent
├── .dockerignore
├── .github/
│   └── workflows/
│       └── ci.yml                   # mvn verify + images Docker + build frontend
├── docs/
│   ├── ARCHITECTURE.md              # architecture implémentée + argumentaire
│   ├── GLOSSAIRE.md                 # vocabulaire métier imposé
│   ├── ROADMAP.md                   # plan d'action et jalons
│   ├── plan-de-tests.md             # typologie, jeux d'essai, exécution
│   ├── alerting.md                  # règles, seuils, fréquence, contenu des e-mails
│   ├── mini-pc-deploy.md            # déploiement de la démo (mini PC + NAS)
│   ├── grille-evaluation.md         # grille du jury (source)
│   └── sujet.md                     # cahier des charges (source)
├── futurekawa-lib/                  # contrat d'API partagé (DTO + enums)
│   ├── pom.xml
│   └── src/main/java/com/futurekawa/lib/
│       ├── dto/request/
│       ├── dto/response/
│       └── enums/
├── backend-local/                   # un déploiement par pays
│   ├── pom.xml
│   ├── api/openapi.yml              # spécification générée par les tests
│   ├── src/
│   ├── mosquitto/mosquitto.conf
│   ├── scripts/run-dev.sh
│   ├── Dockerfile                   # image runtime (jar prébuild)
│   ├── Dockerfile.prod              # image multi-stage (build depuis la racine)
│   ├── docker-compose.yml           # backend seul, base et broker externes
│   ├── docker-compose.prod.yml      # stack pays complète : postgres + mosquitto + backend
│   ├── .env.example
│   └── .env.prod.example
├── backend-central/                 # consolidation siège
│   ├── pom.xml
│   ├── api-contract.md
│   ├── src/
│   ├── scripts/run-dev.sh
│   ├── Dockerfile
│   ├── Dockerfile.prod
│   ├── docker-compose.yml           # stack siège
│   └── .env.example
├── frontend-web/                    # poste de supervision (Vue 3 + Vite)
│   ├── package.json
│   ├── index.html
│   ├── public/
│   ├── src/
│   │   ├── api/                     # clients HTTP du backend central
│   │   ├── components/
│   │   ├── mocks/                   # fixtures MSW pour le dev hors-ligne
│   │   ├── stores/
│   │   ├── types/api.ts             # miroir TypeScript de futurekawa-lib
│   │   └── views/
│   └── .env.example
├── odoo/
│   ├── addons/
│   │   └── futurekawa_quality/      # fiches de non-conformité + e-mail
│   └── implementation_plan.md       # futurekawa_inventory : spécifié, non implémenté
├── iot/
│   ├── arduino-uno/
│   │   └── futurekawa_dht22.ino     # firmware : DHT22 → JSON sur port série
│   ├── serial-bridge/
│   │   └── serial_mqtt_bridge.py    # pont série → MQTT
│   ├── README.md                    # câblage, protocole, mise en service
│   └── .env.example
└── tests-e2e/
    ├── conftest.py                  # page-object + fixtures Selenium
    ├── pytest.ini                   # marqueurs et options par défaut
    ├── test_ui.py                   # recette d'interface (31 scénarios)
    └── run-tests.sh                 # build + serveur + recette, en une commande
```

## Lecture de l'arborescence

- `pom.xml` à la racine est l'agrégateur Maven (`futurekawa-parent`) : il construit les
  trois modules Java en un seul `mvn verify` et centralise la version de Java, les
  plugins et les versions partagées.
- `futurekawa-lib/` porte le **contrat d'API partagé** : les records DTO (`*Response`,
  `CreateLotRequest`, `UpdateLotRequest`, `UpdateAlerteRequest`) et les enums métier
  (`StatutLot`, `StatutAlerte`, `NiveauAlerte`, `TypeAlerte`), utilisés **à la fois** par
  `backend-local` et `backend-central`. Un type n'y monte que si les deux services Java
  l'utilisent ; le module ne dépend ni de Spring, ni de JPA, ni de Jackson.
- `backend-local/` contient un codebase unique déployé une fois par pays. La configuration
  pays (seuils, tolérances, identité, connexions) est injectée par `.env`.
- `backend-central/` porte la consolidation siège : fan-out REST vers les backends pays,
  regroupement par `codePays`, et un circuit breaker par pays.
- `frontend-web/` héberge l'interface de consultation du siège. `VITE_USE_MOCKS=true`
  bascule sur les fixtures MSW pour travailler sans backend.
- `odoo/` contient le module ERP `futurekawa_quality` (fiches de non-conformité, workflow
  d'état, e-mail au service qualité sur alerte critique).
- `iot/` contient le firmware Arduino Uno et le pont série → MQTT qui le relaie.
- `tests-e2e/` porte la recette d'interface automatisée (Selenium + pytest), jouée sur le
  frontend construit avec les fixtures MSW.

## Démarrage rapide

### Prérequis

JDK 25, Maven 3.9+, Docker avec Compose, Node 20+ et pnpm (ou npm) pour le frontend.

### Construire et tester toute la chaîne Java

```bash
mvn clean verify          # 3 modules, 190 tests, barrière JaCoCo à 80 % sur les deux services
```

### Lancer un backend pays

```bash
cd backend-local
cp .env.prod.example .env                                  # puis renseigner le pays
docker compose -f docker-compose.prod.yml up -d --build     # postgres + mosquitto + backend
```

L'API du pays écoute sur `http://localhost:8081` (`/swagger-ui.html` pour l'explorer).

En développement, avec une base et un broker déjà disponibles :

```bash
./backend-local/scripts/run-dev.sh
```

### Lancer le siège

```bash
cd backend-central
cp .env.example .env      # URL des backends pays
docker compose up -d --build
```

L'API consolidée écoute sur `http://localhost:8090`. En développement :
`./backend-central/scripts/run-dev.sh`.

### Lancer le frontend

```bash
cd frontend-web
cp .env.example .env      # VITE_USE_MOCKS=false, VITE_PROXY_TARGET=http://localhost:8090
pnpm install && pnpm dev
```

L'interface est servie sur `http://localhost:5173`. Le proxy Vite relaie `/api/v1` vers le
backend central, donc le navigateur ne voit qu'une seule origine et CORS n'entre pas en jeu.

### Lancer la recette d'interface

```bash
tests-e2e/run-tests.sh    # construit le frontend, le sert, joue 31 scénarios Selenium
```

Le script se charge des dépendances et du serveur ; seul Chrome (ou Chromium) doit être
installé, Selenium Manager récupère le pilote. La recette tourne sur les fixtures MSW, donc
elle ne demande ni backend pays, ni base, ni broker. Contre un frontend déjà démarré :
`E2E_BASE_URL=http://localhost:5173 pytest tests-e2e -v`.

## Ajouter un pays

Le nombre de pays n'est pas inscrit dans le code. Pour en ajouter un :

1. **Déployer sa stack pays** : copier `backend-local/.env.prod.example` en `.env`, y mettre
   son `COUNTRY_CODE`, son `COUNTRY_NAME`, ses seuils et tolérances, puis
   `docker compose -f docker-compose.prod.yml up -d --build`.
2. **Le déclarer au siège** : ajouter une paire `CODE=url` à `FUTUREKAWA_LOCALS` dans
   `backend-central/.env`, puis redémarrer le central. La liste est ouverte :
   `FUTUREKAWA_LOCALS=BR=http://backend-local-br:8081,PE=http://backend-local-pe:8081`.
   Le central vérifie au démarrage que le backend renvoie bien le code annoncé.
3. **Renseigner son URL dans Odoo** : le pays s'inscrit tout seul dans
   *FutureKawa Quality → Configuration → Pays* dès sa première alerte, avec son nom (le
   backend l'envoie avec le code). Il reste à saisir l'URL de son backend pour que les
   décisions qualité (analyse, validation, déclassement) lui soient répercutées ; les pays
   sans URL sont signalés dans la liste. Vous pouvez aussi créer le pays à l'avance et
   renseigner son URL avant sa première alerte.

Le frontend n'a rien à changer : il découvre les pays et leurs seuils via
`GET /api/v1/pays`.

## État du projet

Ce qui fonctionne de bout en bout : acquisition capteur → MQTT → persistance → détection
d'alerte → fiche Odoo → e-mail au service qualité, avec consultation par le frontend via le
backend central. Les écarts connus et les chantiers restants sont listés dans
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §7.
