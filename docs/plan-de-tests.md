# Plan de Tests — FutureKawa

Ce document décrit la stratégie de test du projet et son état d'exécution réel, depuis le
backend pays (Java/IoT) jusqu'à l'interface web.

> État au 27/08/2026. Les chiffres cités sont ceux produits par `mvn clean verify` à la
> racine du dépôt.

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
- **Résultat courant** : 74 tests, 0 échec, ~83 % de lignes couvertes.
- **Lacunes connues** : `OdooRpcClient` (appels JSON-RPC HTTP) et les `equals`/`hashCode`
  des entités sont peu couverts.

### 1.4 Tests de recette et bout en bout (UI)

- **Objectif** : simuler le parcours d'un utilisateur du siège (magasinier, qualiticien) et
  vérifier que les données consolidées s'affichent.
- **Outil retenu** : **Selenium** piloté en Python (`tests-e2e/test_ui.py`).

## 2. Jeux d'essai et cas de test (recette fonctionnelle)

| Donnée injectée | Contexte / contrainte | Résultat attendu sur l'interface |
| :--- | :--- | :--- |
| Lot `LOT-BR-001` | Température 29 °C, humidité 55 % (dans la tolérance Brésil) | Le lot s'affiche avec le statut **CONFORME** (pastille verte). Aucune alerte. |
| Lot `LOT-CO-999` | Température 38 °C en Colombie (seuil 26 °C ± 3) | La courbe dépasse le seuil, le lot passe **EN_ALERTE**, une alerte `CONDITION_NON_IDEALE` de niveau `CRITIQUE` apparaît, et une fiche est créée dans Odoo avec envoi d'e-mail. |
| Lot `LOT-EQ-OLD` | Date d'entrée en stockage > 365 jours | Alerte `LOT_TROP_ANCIEN` levée par le job horaire, lot marqué **PERIME**. |
| Backend d'un pays arrêté | Le siège interroge trois pays, un seul est indisponible | La réponse reste `200 OK` avec les pays disponibles, l'en-tête `X-Unavailable-Countries` liste le pays absent et le frontend affiche un bandeau d'avertissement. |

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
cd tests-e2e
pip install -r requirements.txt
python test_ui.py       # nécessite chromedriver et le frontend démarré
```

Le frontend est également contrôlé en CI par `pnpm run lint` et `pnpm run typecheck`
(typage strict `vue-tsc`), puis par un build de production.

## 4. Intégration continue

Le workflow `.github/workflows/ci.yml` exécute, sur `main` et `develop` et sur chaque
pull request :

1. `mvn -B clean verify` sur le réacteur complet (les trois modules Java), donc les 68
   tests et la barrière de couverture ;
2. la construction des deux images Docker de production ;
3. le lint, le typecheck et le build du frontend ;
4. la publication des jars, des rapports JaCoCo et du `dist/` frontend comme artefacts.

## 5. Limites connues du dispositif

À corriger avant la soutenance, par ordre d'importance :

1. **`tests-e2e/test_ui.py` ne vérifie presque rien** : le script ouvre le tableau de bord
   et lit le titre de la page, mais les assertions sur le contenu (présence de l'alerte du
   lot `LOT-CO-999`) sont commentées. Il faut ajouter des identifiants stables aux
   composants Vue puis activer ces assertions.
2. **`backend-central` n'a aucun test** : le plugin JaCoCo est en place mais sans barrière,
   précisément parce qu'un seuil échouerait faute de tests. Les cibles prioritaires sont le
   fan-out (un pays en panne est omis, l'en-tête est posé), le routage par `codePays`, et le
   mapping `CreateLotRequest` → contrat local.
3. **Aucun test unitaire de composant frontend** : pas de Vitest ni de Vue Test Utils, seuls
   le lint, le typecheck et le build sont contrôlés.
4. **Le frontend n'exploite pas encore les filtres serveur** : il charge `size=100` par pays
   puis filtre en mémoire. Les filtres et le tri FIFO fonctionnent et sont testés côté
   backend, mais au-delà de 100 lots par pays l'affichage porte sur un sous-ensemble.
5. **Le retour Odoo → backend n'est pas couvert par un test automatisé** : il a été validé
   manuellement (appel `PATCH` identique à celui du module, alerte clôturée, détection
   réarmée). Un test Odoo (`odoo -i futurekawa_quality --test-enable`) reste à écrire.
