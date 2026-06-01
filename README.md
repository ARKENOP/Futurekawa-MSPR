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
│   ├── api/
│   ├── src/
│   ├── tests/
│   ├── docker-compose.yml
│   ├── .env.bresil
│   ├── .env.equateur
│   └── .env.colombie
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
├── odoo/
│   └── addons/
│       ├── auth_oidc_server/       
│       ├── futurekawa_quality/     
│       └── futurekawa_inventory/   
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
│   └── postgres/
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
- `backend-local/` contient un codebase unique pour la logique locale MQTT + SQL + alerting. La configuration par pays (seuils, noms, identifiants) est injectée via un fichier `.env` (`.env.bresil`, `.env.equateur`, `.env.colombie`).
- `backend-central/` porte la consolidation siège et les appels REST vers les backends locaux.
- `frontend-web/` héberge l'interface de consultation centralisée pour le siège et les équipes métiers.
- `odoo/` contient les modules ERP personnalisés (Python/XML) pour :
  - **Auth OIDC** : Fournisseur central d'authentification SSO et claims de rôles.
  - **Qualité** : Génération automatique de fiches de non-conformité en cas d'alerte.
  - **Inventaire** : Moteur de recommandation des lots à expédier en priorité (logique FIFO).
- `iot/` contient le prototype embarqué ESP32 et les formats de messages MQTT.
- `infra/` centralise la conteneurisation, l'intégration continue Jenkins, le broker MQTT et la base SQL.
- `tests/` regroupe les scénarios transverses d'intégration et de bout en bout.
- `scripts/` fournit les commandes de lancement, de build et de test.
