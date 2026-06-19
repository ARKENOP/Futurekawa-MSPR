# ROADMAP - FutureKawa MSPR

## 1. Objectif du plan

Cette roadmap transforme le cahier des charges FutureKawa en plan d’action exécutable, en priorisant les exigences qui démontrent le plus clairement les critères de la grille d’évaluation.

La logique retenue suit l’architecture cible de référence:

- un backend local par pays, conteneurisé, avec base SQL, broker MQTT et alerting;
- un backend central au siège pour la consolidation multi-pays;
- un frontend web centralisé pour la consultation des stocks, des mesures de stockage et des alertes;
- un prototype loT pour l’émission des mesures de température et d’humidité;
- un socle qualité composé de tests, CI/CD, documentation et préparation au changement.

## 2. Priorités issues de la grille d’évaluation

La grille d’évaluation prime sur le cahier des charges. Les priorités de construction sont donc les suivantes:

1. Démontrer la collecte des besoins métiers et leur formalisation.
2. Justifier une architecture distribuée pays + siège, stable et pérenne.
3. Produire une solution applicative complète et démontrable.
4. Intégrer un module loT fonctionnel avec persistance et consultation.
5. Mettre en place des tests exploitables et une chaîne d’intégration continue.
6. Rédiger une documentation utilisateur et technique claire.
7. Préparer le changement autour d’un futur automatisme d’entrepôt.

## 3. Plan d’action par phases

### Phase 0 - Cadrage fonctionnel et validation du périmètre

Objectif: stabiliser le besoin et verrouiller le vocabulaire métier avant de développer.

Actions:

- formaliser l’interview-type et le questionnaire de collecte des besoins;
- lister les besoins métiers attendus: gestion des pays, exploitations, entrepôts, lots, mesures de stockage, alertes et consultation siège;
- expliciter les contraintes métier: FIFO, traçabilité, alertes automatiques, multi-pays, réseau variable;
- figer les statuts canoniques: `conforme`, `en_alerte`, `perime` pour les lots, et `ouverte`, `notifiee`, `cloturee` pour les alertes;
- valider les hypothèses techniques déjà visibles dans l’architecture cible.

Livrables attendus:

- questionnaire de cadrage;
- synthèse des besoins métiers;
- liste des règles métier et des seuils d’alerte.

Critère de réussite:

- chaque besoin du sujet est relié à un usage métier concret et à un composant de la solution.

### Phase 1 - Architecture applicative et socle technique

Objectif: prouver la cohérence globale du système avant d’empiler les fonctionnalités.

Actions:

- documenter l’architecture pays + siège en s’appuyant sur le schéma existant;
- justifier le découpage entre backend local et backend central;
- préciser les flux d’échange: MQTT pour les mesures, REST pour les consultations et la consolidation, SMTP pour les notifications;
- définir les principes de robustesse: conteneurisation, séparation des responsabilités, tolérance aux pannes, circuit breaker côté siège, historisation en base SQL;
- cadrer la structure de déploiement Docker Compose pour le pays et pour le siège.

Livrables attendus:

- note d’architecture argumentée;
- schéma de fonctionnement consolidé;
- conventions de nommage alignées sur le glossaire.

Critère de réussite:

- l’architecture répond explicitement aux exigences de stabilité, d’efficacité et de pérennité.

### Phase 2 - Backend local pays

Objectif: livrer le socle opérationnel d’un pays exemple.

Actions:

- implémenter la gestion des lots avec tri FIFO par date de stockage;
- persister les lots et les mesures de stockage dans une base SQL;
- consommer les mesures publiées par le broker MQTT local;
- calculer les écarts par rapport aux seuils pays pour détecter les situations à risque;
- déclencher les alertes et préparer l’envoi d’email au responsable d’exploitation;
- exposer une API REST locale pour la consultation des stocks, des mesures et des alertes.

Livrables attendus:

- backend local conteneurisé;
- schéma de données;
- endpoints REST documentés;
- règles d’alerte documentées.

Critère de réussite:

- un pays exemple fonctionne de bout en bout: réception des mesures, stockage, alerte et consultation.

### Phase 3 - Frontend siège et backend central

Objectif: donner une vue consolidée multi-pays au siège.

Actions:

- développer le backend central pour interroger les backends locaux via des routes REST;
- consolider les stocks, les historiques de mesures et les alertes par pays;
- exposer les données au frontend sous une forme simple à consommer;
- réaliser l’interface Web de consultation: sélection d’un pays, liste des lots triés par date de stockage, sélection d’un lot, affichage des courbes de température et d’humidité, lecture des alertes;
- conserver une interface lisible pour les utilisateurs terrain et exploitable par le siège.

Livrables attendus:

- backend central;
- frontend Web du siège;
- visualisation des historiques de mesures;
- vue consolidée par pays et par entrepôt.

Critère de réussite:

- le siège peut piloter et consulter sans accéder directement aux backends locaux.

### Phase 4 - Module loT et démonstration technique

Objectif: prouver la chaîne d’acquisition des mesures depuis le terrain.

Actions:

- documenter le microcontrôleur et le capteur retenus;
- publier les mesures température / humidité vers le broker MQTT local;
- définir le format des messages et la fréquence d’envoi;
- sécuriser la logique de reconnexion et de reprise;
- intégrer la démonstration du flux capteur -> broker -> backend -> base -> frontend.

Livrables attendus:

- prototype fonctionnel du module loT;
- description du câblage et du protocole MQTT;
- preuve de réception et de consultation des mesures.

Critère de réussite:

- la soutenance peut montrer une mesure réelle ou un scénario reproductible complet.

### Phase 5 - Tests et intégration continue

Objectif: démontrer la testabilité et l’industrialisation minimale de la solution.

Actions:

- rédiger le plan de test en distinguant tests unitaires, d’intégration, API, UI et bout en bout;
- définir les jeux de données, les résultats attendus et les critères de succès;
- mettre en place des tests automatisés sur le backend et le frontend;
- intégrer un pipeline CI/CD Jenkins pour lancer build, tests et packaging;
- produire des artefacts exploitables pour la démo.

Livrables attendus:

- plan de test détaillé;
- exécution des tests manuels et automatisés;
- Jenkinsfile ou configuration Jenkins documentée;
- preuve d’exécution de la CI.

Critère de réussite:

- les tests couvrent les fonctions critiques et s’exécutent de manière répétable.

### Phase 6 - Documentation et conduite du changement

Objectif: rendre la solution compréhensible par les métiers et défendable devant le jury.

Actions:

- rédiger la documentation utilisateur orientée exploitation, siège et qualité;
- comparer la réalisation technique au cahier des charges;
- décrire les alertes, les seuils, les actions attendues et la lecture des courbes;
- construire le prototype de schéma pour la phase 2 d’automatisation des entrepôts;
- préparer le questionnaire de cadrage pour l’interview suivante;
- structurer le support de soutenance en reprenant les points de la grille d’évaluation.

Livrables attendus:

- documentation utilisateur;
- documentation technique;
- schéma de principe de l’automatisation future;
- questionnaire phase 2;
- support de soutenance.

Critère de réussite:

- un utilisateur métier comprend le fonctionnement, les alertes et les limites de la solution.

## 4. Matrice de couverture des critères de la grille

| Critère | Couverture attendue | Livrables principaux |
| --- | --- | --- |
| 1. Collecte des besoins | Interview-type, questionnaire, synthèse métier | Cadrage fonctionnel |
| 2. Architecture applicative | Justification du découpage pays + siège, schéma, robustesse | Note d’architecture |
| 3. Développement d’une application adaptée | Backend local, backend central, frontend, intégration loT | Code source et démo |
| 4. Progiciel intégré | Hors périmètre principal, à mentionner comme non retenu dans le cadrage | Note de positionnement |
| 5. Tests | Plan de test, tests automatisés et manuels, jeux d’essai | Dossier de tests |
| 6. Intégration continue | Pipeline Jenkins, build, tests, packaging | Jenkinsfile et preuve d’exécution |
| 7. Documentation utilisateur | Guide métier, FAQ, lecture des alertes et courbes | Documentation utilisateur |
| 8. Conduite du changement | Questionnaire phase 2, schéma d’automatisation, supports de communication | Dossier de changement |

## 5. Jalons de validation

1. Jalon 1: besoin cadré, vocabulaire figé, architecture validée.
2. Jalon 2: backend local pays fonctionnel avec stockage et alertes.
3. Jalon 3: backend central et frontend capables de consolider les pays.
4. Jalon 4: prototype loT démontré de bout en bout.
5. Jalon 5: tests, CI/CD et documentation finalisés.
6. Jalon 6: préparation soutenance et démonstration technique.

## 6. Risques à surveiller

- dérive de périmètre si le besoin phase 2 prend le pas sur la livraison MSPR;
- incohérence terminologique si le glossaire n’est pas appliqué partout;
- démonstration loT fragilisée si le prototype matériel n’est pas stabilisé tôt;
- dette de test si les scénarios critiques ne sont pas automatisés;
- dépendance trop forte au réseau si le mode dégradé pays n’est pas anticipé.

## 7. Ordre recommandé d’exécution

1. Verrouiller le cadrage et le vocabulaire.
2. Valider l’architecture et les flux.
3. Construire d’abord le backend local pays.
4. Ajouter le frontend siège et le backend central.
5. Brancher le prototype loT et les alertes.
6. Finaliser tests, CI/CD et documentation.
7. Préparer la soutenance à partir de la grille de validation.
