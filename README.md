# FutureKawa MSPR

## Arborescence du projet

```text
Futurekawa-MSPR/
├── README.md
├── .gitignore
├── .github/
│   └── workflows/
│       └── ci.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── GLOSSAIRE.md
│   ├── grille-evaluation.md
│   ├── ROADMAP.md
│   ├── sujet.md
│   ├── dossier-technique.md
│   ├── documentation-utilisateur.md
│   ├── plan-de-tests.md
│   ├── conduite-changement.md
│   └── support-soutenance.md
├── backend-local/
│   ├── bresil/
│   │   ├── api/
│   │   ├── src/
│   │   ├── tests/
│   │   └── docker-compose.yml
│   ├── equateur/
│   │   ├── api/
│   │   ├── src/
│   │   ├── tests/
│   │   └── docker-compose.yml
│   ├── colombie/
│   │   ├── api/
│   │   ├── src/
│   │   ├── tests/
│   │   └── docker-compose.yml
│   └── shared/
│       ├── contrats-openapi/
│       └── scripts/
├── backend-central/
│   ├── api/
│   ├── src/
│   ├── tests/
│   └── docker-compose.yml
├── frontend-web/
│   ├── src/
│   ├── public/
│   ├── tests/
│   └── docker-compose.yml
├── iot/
│   ├── esp32/
│   │   ├── firmware/
│   │   ├── schemas/
│   │   └── README.md
│   └── payloads-mqtt/
├── infra/
│   ├── docker/
│   ├── jenkins/
│   │   └── Jenkinsfile
│   ├── mosquitto/
│   ├── postgres/
│   └── smtp/
├── tests/
│   ├── integration/
│   ├── end-to-end/
│   └── fixtures/
└── scripts/
	├── build/
	├── run/
	└── test/
```

## Lecture de l'arborescence

- `docs/` regroupe les livrables de cadrage, d'architecture, de tests, de documentation utilisateur et de conduite du changement.
- `backend-local/` contient la partie pays, avec un sous-dossier par pays pour refléter l'architecture distribuée et la logique locale MQTT + SQL + alerting.
- `backend-central/` porte la consolidation siège et les appels REST vers les backends locaux.
- `frontend-web/` héberge l'interface de consultation centralisée pour le siège et les équipes métiers.
- `iot/` contient le prototype embarqué ESP32 et les formats de messages MQTT.
- `infra/` centralise la conteneurisation, l'intégration continue Jenkins, le broker MQTT, la base SQL et le serveur SMTP.
- `tests/` regroupe les scénarios transverses d'intégration et de bout en bout.
- `scripts/` fournit les commandes de lancement, de build et de test.

