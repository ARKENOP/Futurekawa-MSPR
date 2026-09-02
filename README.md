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
├── docker-compose.siege.yml         # stack siège : frontend + backend-central + Odoo
├── docker-compose.local.yml         # stack pays : mosquitto + backend-local + postgres
├── .env.siege.example               # gabarit d'environnement de la stack siège
├── .env.local.example               # gabarit d'environnement d'une stack pays
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
│   ├── docker-compose.yml           # hérité : backend seul, base et broker externes
│   ├── docker-compose.prod.yml      # hérité : remplacé par docker-compose.local.yml
│   ├── .env.example
│   └── .env.prod.example
├── backend-central/                 # consolidation siège
│   ├── pom.xml
│   ├── api-contract.md
│   ├── src/
│   ├── scripts/run-dev.sh
│   ├── Dockerfile
│   ├── Dockerfile.prod
│   ├── docker-compose.yml           # hérité : remplacé par docker-compose.siege.yml
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
│   ├── Dockerfile                   # image multi-stage : build Vite → nginx
│   ├── nginx.conf                   # SPA + reverse proxy /api/v1 → backend-central
│   └── .env.example
├── odoo/
│   ├── addons/
│   │   └── futurekawa_quality/      # fiches de non-conformité + e-mail
│   ├── odoo.conf                    # configuration serveur (addons_path, proxy_mode)
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

### Les deux stacks du SI

Le système se déploie en deux moitiés, chacune décrite par un fichier Compose à la racine :

| Stack | Fichier | Services | Où elle tourne |
| --- | --- | --- | --- |
| Siège | `docker-compose.siege.yml` | `frontend-web` + `backend-central` + `odoo` + `odoo-db` | une fois, au siège |
| Pays | `docker-compose.local.yml` | `mosquitto` + `backend-local` + `postgres` | une fois par pays, sur le site |

Les deux fichiers se lancent avec `--env-file` : le fichier d'environnement sert **à la
fois** de source d'interpolation pour le câblage Compose et d'environnement injecté dans
le backend.

### Lancer la stack siège

```bash
cp .env.siege.example .env.siege     # mots de passe Odoo, liste FUTUREKAWA_LOCALS
docker compose --env-file .env.siege -f docker-compose.siege.yml up -d --build
```

| Service | URL | Rôle |
| --- | --- | --- |
| `frontend-web` | http://localhost:8080 | poste de supervision |
| `backend-central` | http://localhost:8090/swagger-ui.html | API consolidée |
| `odoo` | http://localhost:8069 | fiches de non-conformité |

Le frontend est servi par nginx, qui relaie lui-même `/api/v1` vers `backend-central` :
le navigateur ne voit qu'une seule origine et CORS n'entre pas en jeu, exactement comme le
proxy Vite en développement. Au premier démarrage, créer la base Odoo depuis
http://localhost:8069 (mot de passe maître dans `odoo/odoo.conf`), puis installer le module
*FutureKawa Quality* — le dossier `odoo/addons/` est monté dans le conteneur.

### Lancer une stack pays

```bash
cp .env.local.example .env.local     # COUNTRY_CODE, seuils, tolérances, secrets
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
```

L'API du pays écoute sur `http://localhost:8081` (`/swagger-ui.html` pour l'explorer) et le
broker MQTT sur le port `1883`. Odoo n'est pas dans cette stack : il vit au siège, et
`ODOO_URL` dans `.env.local` doit pointer vers une adresse joignable depuis le site.

Le pont série (`iot/serial-bridge`) n'est pas conteneurisé : il a besoin du port USB de la
machine sur laquelle l'Arduino est branché et tourne donc directement sur l'hôte du pays,
en publiant vers le broker sur `1883`.

### Lancer un service seul, en développement

Avec une base et un broker déjà disponibles :

```bash
./backend-local/scripts/run-dev.sh
./backend-central/scripts/run-dev.sh
```

```bash
cd frontend-web
cp .env.example .env      # VITE_USE_MOCKS=false, VITE_PROXY_TARGET=http://localhost:8090
pnpm install && pnpm dev
```

L'interface est servie sur `http://localhost:5173`. `VITE_USE_MOCKS=true` bascule sur les
fixtures MSW pour travailler sans aucun backend.

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

1. **Déployer sa stack pays** : copier `.env.local.example` en `.env.local`, y mettre
   son `COUNTRY_CODE`, son `COUNTRY_NAME`, ses seuils et tolérances, puis
   `docker compose --env-file .env.local -f docker-compose.local.yml up -d --build`.
2. **Le déclarer au siège** : ajouter une paire `CODE=url` à `FUTUREKAWA_LOCALS` dans
   `.env.siege`, puis redémarrer le central. La liste est ouverte :
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
