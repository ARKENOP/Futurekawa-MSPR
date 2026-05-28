```mermaid
graph TB
    %% Style global
    classDef hq fill:#e3f2fd,stroke:#1565c0,stroke-width:2px;
    classDef local fill:#efebe9,stroke:#4e342e,stroke-width:2px;
    classDef hardware fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef db fill:#fff3e0,stroke:#ef6c00,stroke-width:2px;
    classDef erp fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef docker fill:#e0f7fa,stroke:#00acc1,stroke-width:1px,stroke-dasharray: 5 5;
    classDef devops fill:#fafafa,stroke:#424242,stroke-width:1px,stroke-dasharray: 3 3;

    subgraph CentralHQ [🏢 ENVIRONNEMENT CENTRAL - LE SIÈGE]
        direction TB
        
        subgraph HQ_Docker [🐳 Orchestration : Docker Compose HQ]
            Angular[🖥️ Frontend Web<br>Angular + Chart.js<br>Affichage FIFO & Courbes]:::hq
            SBCentral[☕ Backend Central<br>Spring Boot + Resilience4j<br>Consolidation & Contrats OpenAPI]:::hq
        end
        
        ERP[ERP <br>Salesforce ou SAP<br>+ Module Spécifique développé en Apex/ABAP]:::erp
    end

    subgraph LocalCountry [☕ ENVIRONNEMENT LOCAL - PAR PAYS ex: Brésil]
        direction TB
        
        ESP32[📟 Module IoT<br>ESP32 + Capteur DHT22]:::hardware
        
        subgraph Local_Docker [🐳 Déploiement Isolé : Docker Compose Local]
            Mosquitto[📡 Broker MQTT<br>Eclipse Mosquitto]:::hardware
            SBLocal[☕ Backend Local<br>Spring Boot<br>Contrats OpenAPI & Alertes]:::local
            Postgres[(🐘 BDD Locale<br>PostgreSQL<br>Lots & Historique)]:::db
            SMTP[✉️ Serveur Alerte<br>Notification Email]:::local
        end
    end

    subgraph DevOps [⚙️ USINE LOGICIELLE & QUALITÉ - CI/CD]
        direction LR
        Git[Versionning<br>Git]:::devops
        GithubActions[Pipeline CI/CD<br>Github Actions]:::devops
        Tests[Outils de Testing<br>Playwright / JUnit]:::devops
        Git --> GithubActions --> Tests
    end

    %% Flux de données Internes au Pays (Temps Réel)
    ESP32 -->|1. Publication Mesures<br>MQTT over TLS JSON| Mosquitto
    Mosquitto -->|2. Écoute / Flux Continu<br>Spring Integration MQTT| SBLocal
    SBLocal -->|3. Persistance ORM<br>Spring Data JPA| Postgres
    SBLocal -->|4. Notification Automatique<br>SMTP / MimeMessage| SMTP

    %% Flux Inter-Réseaux (Réseau Variable & Tolérance aux pannes)
    Angular -->|5. Requêtes Utilisateur<br>HTTPS / REST| SBCentral
    SBCentral -.->|6. Collecte REST synchrone<br>HTTPS / JSON + Circuit Breaker| SBLocal

    %% Flux d'Intégration Applicative Métier (ERP)
    SBCentral <-->|7. Transmission Alertes/Stocks<br>REST vers Endpoint ERP Custom| ERP

    %% Application des styles
    class CentralHQ hq;
    class LocalCountry local;
    class Local_Docker,HQ_Docker docker;
    ```