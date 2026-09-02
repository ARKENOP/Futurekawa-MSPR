# Plan de Tests — FutureKawa

Ce document décrit la stratégie de test du projet et son état d'exécution réel, depuis le
backend pays (Java/IoT) jusqu'à l'interface web.

> État au 02/09/2026. Les chiffres cités sont ceux produits par `mvn clean verify` à la
> racine du dépôt et par `tests-e2e/run-tests.sh`.

## 1. Typologie des tests

### 1.1 Tests unitaires (backend)

- **Objectif** : valider la logique métier isolée — détection de dépassement de seuil,
  déduplication d'alerte, péremption des lots, mapping entité ↔ DTO.
- **Outils** : JUnit 5 + Mockito, sans contexte Spring.
- **Périmètre** : `LotServiceTest`, `AlerteServiceTest`, `MesureServiceTest`,
  `PaysServiceTest`, `EntrepotServiceTest`, `ExploitationServiceTest`,
  `PeremptionSchedulerTest`, `MqttMessageHandlerTest`, `OdooQualityAlertServiceTest`,
  `MapperTest`.

### 1.2 Tests d'intégration (API et base de données)

- **Objectif** : vérifier la chaîne complète contrôleur → service → repository → base, et
  le contrat d'erreur RFC 7807.
- **Outils** : `@SpringBootTest(RANDOM_PORT)` avec un `RestClient` qui attaque l'application
  en HTTP réel, profil `test`.
- **Base de test : H2 en mode compatibilité PostgreSQL** (`MODE=PostgreSQL`), pas
  Testcontainers. Décision assumée : Testcontainers échoue sur la version de Docker de la
  machine de développement (négociation d'API entre Docker 29 et le client docker-java
  embarqué). Le code n'utilise que du JPA/JPQL standard, sans SQL natif PostgreSQL, donc
  H2 couvre le même périmètre — au prix de ne pas valider les spécificités du moteur.
  `mvn verify` ne demande donc **ni base externe ni démon Docker**.
- **Périmètre** : `LotApiIntegrationTest`, `MesureAlerteApiIntegrationTest`,
  `EntrepotExploitationApiIntegrationTest`, `PaysApiIntegrationTest`,
  `LotRepositoryIntegrationTest`, `OpenApiExportTest` (qui régénère
  `backend-local/api/openapi.yml`).
- Couvrent notamment les règles du contrat corrigées le 27/08/2026 : conservation de la
  date d'entrée en stockage soumise, calcul de `ancienneteJours`, tri FIFO par défaut,
  filtres `statutLot` / `statutAlerte` / `typeAlerte`, fenêtre `from`/`to` des mesures et
  horodatage de clôture d'alerte.
- Le broker MQTT n'est pas requis : `MqttConfig` est désactivé dans le profil de test
  (`futurekawa.mqtt.enabled=false`), l'ingestion étant couverte au niveau unitaire par
  `MqttMessageHandlerTest`.

### 1.3 Couverture

- **Outil** : JaCoCo 0.8.13.
- **Barrière** : 80 % de lignes sur le bundle `backend-local`, liée à la phase `verify`
  (donc `mvn test` reste vert, seul `mvn verify` bloque). Sont exclus la classe de
  démarrage, le câblage de configuration (`MqttConfig`, `RestClientConfig`, `OpenApiConfig`)
  et les records `*Properties`.
- **Barrière** : la même règle de 80 % s'applique désormais aux **deux** services.
- **Résultat courant** :

  | Module | Tests | Couverture lignes |
  | --- | --- | --- |
  | `backend-local` | 86 | ~83 % |
  | `backend-central` | 104 | ~95 % |
  | **Total Java** | **190** | barrière 80 % tenue sur les deux bundles |

- **Lacunes connues** : `OdooRpcClient` (appels JSON-RPC HTTP) et les `equals`/`hashCode`
  des entités sont peu couverts.

### 1.4 Tests du siège (backend-central)

- **Objectif** : valider ce qui distingue le siège d'un backend pays — la consolidation
  multi-pays, la tolérance à la panne d'un pays, et le routage des écritures.
- **Outils** : JUnit 5 + AssertJ pour l'unitaire ; `@SpringBootTest(RANDOM_PORT)` avec un
  `RestClient` réel pour l'intégration ; `MockRestServiceServer` pour le client HTTP.
- **Choix de substitution** : les backends pays sont remplacés par
  `StubLocalBackendClient`, **au niveau du client, pas du réseau**. Le fan-out, les
  disjoncteurs, les enveloppes consolidées et la traduction des erreurs restent donc du
  code de production ; seul le fil est simulé. Cela permet aussi de dire précisément ce
  que fait un pays : répondre normalement, refuser une ressource par un 404, ou être
  injoignable.
- **Périmètre unitaire** : `LocalBackendPropertiesTest` (registre `CODE=url`, échec rapide
  sur doublon ou entrée malformée), `CountryRegistryTest`, `UnitaryCallExecutorTest`,
  `CountryFanoutExecutorTest`, `CountryDiscoverySchedulerTest`, `ResponseHeadersTest`,
  `CreateLotRequestTest`, `RestClientLocalBackendClientTest` (URLs et paramètres réellement
  émis).
- **Périmètre intégration** : `ConsolidationApiIntegrationTest` (groupement par pays,
  `nomPays` issu de la découverte, ordre FIFO préservé, filtres relayés),
  `ResilienceApiIntegrationTest` (pays en panne → 200 + `X-Unavailable-Countries`, 404 du
  pays relayé tel quel, ouverture du disjoncteur, isolement entre pays),
  `WriteRoutingApiIntegrationTest` (création dans le bon pays, `codePays` non transmis au
  backend, validation, clôture d'alerte).
- **Non-régressions verrouillées** : un 404 répété (entrepôt sans mesure) ne doit jamais
  ouvrir le disjoncteur d'un pays sain — c'est le défaut qui faisait disparaître le Brésil
  de toute l'API consolidée au bout de cinq chargements du tableau de bord.

### 1.5 Tests de recette et bout en bout (UI)

- **Objectif** : simuler le parcours d'un utilisateur du siège (magasinier, qualiticien) et
  vérifier que les données consolidées s'affichent.
- **Outil retenu** : **Selenium** piloté en Python via pytest (`tests-e2e/`).
- **Isolation** : la recette tourne sur le frontend construit avec `VITE_USE_MOCKS=true`,
  donc sur les fixtures MSW de `frontend-web/src/mocks`. Elle ne demande ni backend pays,
  ni base, ni broker : c'est ce qui la rend déterministe et exécutable en CI. L'intégration
  entre les étages est couverte par les suites Java ci-dessus.
- **Sélecteurs** : les composants Vue portent des attributs `data-testid` stables (et
  `data-reference` / `data-pays` / `data-statut` sur les lignes de lot, `data-niveau` /
  `data-statut` / `data-type` sur les alertes). La recette ne dépend donc ni des classes
  CSS ni des libellés.
- **Résultat courant** : **31 scénarios, 0 échec** (Chrome headless).
- **Périmètre** : accès et périmètre pays (6), tableau de bord et indicateurs (6), lots et
  rotation FIFO (7), cycle de vie des alertes (7), fiche entrepôt (5).

## 2. Jeux d'essai et cas de test (recette fonctionnelle)

| Donnée injectée | Contexte / contrainte | Résultat attendu sur l'interface |
| :--- | :--- | :--- |
| Lot `BR-2026-0188`, entré le 01/09/2026 | Moins de 365 jours, entrepôt dans la tolérance | Statut **CONFORME**, aucune alerte, dernier de la liste FIFO. |
| Lot `BR-2024-0087`, entré le 15/08/2024 | 748 jours de stockage, au-delà de la limite de 365 | La tâche de péremption passe le lot en **PERIME** et lève une alerte `LOT_TROP_ANCIEN` de niveau `CRITIQUE` ; le lot remonte en tête de la liste FIFO. |
| Mesure 36,8 °C sur `Entrepôt Sud A` (BR) | Idéal 29 °C ± 3, donc au-delà de **2 ×** la tolérance | Alerte `CONDITION_NON_IDEALE` de niveau `CRITIQUE`, jauge au rouge, fiche créée dans Odoo et e-mail envoyé. |
| Mesure 51,8 % d'humidité sur `Entrepôt Nord B` (BR) | Idéal 55 % ± 2, hors tolérance mais sous 2 × | Alerte `CONDITION_NON_IDEALE` de niveau `WARNING` (badge « Avertissement »), notifiée elle aussi. |
| Mesure 65,6 % d'humidité sur `Entrepôt Nord A` (EC) | Idéal **60 % ± 2** — les seuils de l'Équateur, pas ceux du Brésil | Alerte `CRITIQUE` mentionnant `(idéale 60,0 %)`, ce qui prouve que chaque pays est jugé sur ses propres seuils. |
| Deuxième mesure hors plage sur le même entrepôt | Une alerte du même type est déjà `OUVERTE` | Aucune nouvelle alerte : la déduplication empêche une alerte toutes les 5 s. Clôturer la fiche réarme la détection. |
| Backend d'un pays arrêté (`docker stop futurekawa-backend-local-ec`) | Le siège interroge deux pays, un seul répond | Réponse `200 OK` avec les pays disponibles, en-tête `X-Unavailable-Countries: EC`, et bandeau « Pays indisponibles (backend local injoignable) : EC ». Au redémarrage, le pays revient. |
| 14 lots BR + 9 lots EC | Page de 8 lignes dans l'interface | Pagination **1 / 3**, tri FIFO **inter-pays** (`BR-2024-0087`, `EC-2025-0031`, `BR-2025-0112`…). |

Les jeux d'essai ci-dessus sont ceux réellement déployés sur le poste de démonstration.
Aucun n'a été inséré en SQL : les lots sont créés par `POST /api/v1/lots` à travers le
backend central, les mesures sont **publiées sur MQTT** (donc l'ingestion, la détection de
seuil, l'alerte et la remontée Odoo s'exécutent pour de vrai), et les exploitations et
entrepôts sont semés par l'application au démarrage (`DataInitializer`, idempotent).

### Injection des jeux d'essai

La détection d'alerte se déclenche à la publication MQTT, ce qui permet de rejouer les
scénarios sans capteur :

```bash
mosquitto_pub -h <broker> -t 'futurekawa/BR/entrepot/1/mesures' \
  -m '{"id_capteur":"test-01","temperature_c":38.0,"humidite_pourcent":58.0,"timestamp":1756288000000}'
```

## 3. Exécution

```bash
mvn clean verify        # unitaires + intégration + couverture, à la racine du dépôt
```

Rapport de couverture : `backend-local/target/site/jacoco/index.html`.

```bash
tests-e2e/run-tests.sh          # construit le frontend, le sert, lance la recette
tests-e2e/run-tests.sh -k fifo  # tout argument pytest est transmis
E2E_HEADED=1 tests-e2e/run-tests.sh   # pour montrer le navigateur en soutenance
```

Le script installe les dépendances, construit le frontend avec les fixtures, le sert sur
`http://localhost:4173`, exécute la recette puis arrête le serveur. Contre un frontend déjà
démarré :

```bash
E2E_BASE_URL=http://localhost:5173 pytest tests-e2e -v
```

Le pilote Chrome est téléchargé automatiquement par Selenium Manager ; seul Chrome ou
Chromium doit être installé.

Le frontend est également contrôlé en CI par `pnpm run lint` et `pnpm run typecheck`
(typage strict `vue-tsc`), puis par un build de production.

## 4. Intégration continue

Le workflow `.github/workflows/ci.yml` exécute, sur `main` et `develop` et sur chaque
pull request :

1. `mvn -B clean verify` sur le réacteur complet (les trois modules Java), donc les 190
   tests et les barrières de couverture des deux services ;
2. la construction des deux images Docker de production ;
3. le lint, le typecheck et le build du frontend ;
4. la **recette d'interface Selenium** (job `recette-ui`), sur le frontend construit avec
   les fixtures — aucun service externe n'est nécessaire ;
5. la publication des jars, des rapports JaCoCo et du `dist/` frontend comme artefacts.

## 5. Anomalies détectées par le dispositif

Écrire ces suites a fait apparaître des défauts réels, tous corrigés et verrouillés par un
test :

| Constat | Correction | Test qui l'empêche de revenir |
| --- | --- | --- |
| Une valeur d'énumération inconnue dans un corps JSON (`statutLot`) renvoyait **500** au lieu de 400, sur les deux API. | `HttpMessageNotReadableException` et `MethodArgumentTypeMismatchException` traitées dans les deux `GlobalExceptionHandler`. | `WriteRoutingApiIntegrationTest.rejectsAnUnknownStatusValueAsABadRequest`, `…rejectsMalformedJsonAsABadRequest`, `…rejectsAnUnknownFilterValueAsABadRequest` |
| La liste des lots du frontend ne triait pas : elle dépendait entièrement de l'ordre renvoyé par chaque pays, donc la consolidation multi-pays n'était pas FIFO. | Tri explicite sur `dateEntreeStockage` dans `LotsView`. | `test_les_lots_sont_listes_du_plus_ancien_au_plus_recent` |
| Les seuils Équateur et Colombie des fixtures de démonstration ne correspondaient pas au cahier des charges (24 °C / 21 °C au lieu de 31 °C / 26 °C). | Fixtures alignées sur le cahier des charges. | `test_la_fiche_affiche_les_deux_jauges_de_conditions` |
| Le commentaire du fan-out affirmait qu'un pays répondant 4xx n'était pas signalé indisponible, alors que le code le signalait bien. | Commentaire corrigé pour décrire le comportement réel et sa raison. | `excludesACountryThatRejectsTheRequestAndStillServesTheOthers` |

## 6. Limites connues du dispositif

À traiter avant la soutenance, par ordre d'importance :

1. **Aucun test unitaire de composant frontend** : pas de Vitest ni de Vue Test Utils. Le
   lint, le typecheck, le build et la recette Selenium couvrent l'interface, mais aucun
   test ne cible un composant isolé.
2. **Le frontend n'exploite pas encore les filtres serveur** : il charge `size=100` par pays
   puis filtre et trie en mémoire. Les filtres et le tri fonctionnent et sont testés des
   deux côtés, mais au-delà de 100 lots par pays l'affichage porte sur un sous-ensemble.
3. **Le retour Odoo → backend n'est pas couvert par un test automatisé** : il a été validé
   manuellement (appel `PATCH` identique à celui du module, alerte clôturée, détection
   réarmée). Un test Odoo (`odoo -i futurekawa_quality --test-enable`) reste à écrire — de
   même que l'envoi d'e-mail, qui est entièrement côté Odoo.
4. **La recette tourne sur les fixtures MSW, pas sur les backends réels.** C'est un choix
   (déterminisme, exécution en CI), mais cela veut dire qu'aucun test automatisé ne
   parcourt la chaîne complète capteur → MQTT → base → alerte → Odoo → frontend. Ce
   scénario reste une démonstration manuelle, décrite au §2.
5. **`OdooRpcClient` n'est pas couvert** : les appels JSON-RPC sortants ne sont validés que
   par la démonstration manuelle.
