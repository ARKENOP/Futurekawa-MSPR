# Futurekawa-MSPR

Futurekawa-MSPR/
│
├── .gitignore
├── README.md               <-- Documentation technique globale & instructions de lancement
├── Jenkinsfile             <-- Pipeline CI/CD global (Build, Tests, Docker)
├── docker-compose.yml      <-- Orchestration de toute la simulation (Siège + Pays)
│
├── docs/                   <-- Livrables textuels et schémas
│   ├── architecture/       <-- Schémas logiques, choix technologiques argumentés
│   ├── cadrage_phase2/     <-- Questionnaire d'interview, schéma capteurs/actionneurs
│   ├── tests/              <-- Plan de test, jeux de données, rapports de test
│   └── changement/         <-- Plan d'accompagnement (Future Wheel, Bridge...)
│
├── iot/                    <-- Partie Embarquée / Hardware
│   ├── src/                <-- Code source du microcontrôleur (C++/Arduino/MicroPython)
│   └── diagram/            <-- Schéma de câblage de la breadboard
│
├── local-country/          <-- Le module réplicable par pays (Brésil, Équateur...)
│   ├── api/                <-- API REST locale (Node.js, Python, Go, etc.)
│   ├── worker-mqtt/        <-- Script/Service qui écoute MQTT et écrit en BDD
│   └── database/           <-- Scripts d'initialisation SQL et migrations
│
└── central-hq/             <-- L'environnement du siège social
    ├── backend/            <-- API Centrale d'agrégation
    └── frontend/           <-- Dashboard de supervision (React, Chart.js...)
