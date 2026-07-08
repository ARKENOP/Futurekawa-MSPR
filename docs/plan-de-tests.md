# Plan de Tests - FutureKawa

Ce document décrit la stratégie de test appliquée au projet FutureKawa, couvrant l'ensemble de la chaîne de valeur applicative : depuis le backend local (IoT/Java) jusqu'au frontend Web. 

## 1. Typologie des tests

Pour garantir la stabilité de l'architecture distribuée et de l'interface, nous avons mis en œuvre trois niveaux de tests :

### 1.1 Tests Unitaires (Backend)
- **Objectif** : Valider la logique métier isolée de chaque service (ex: service de détection des péremptions, service de calcul de FIFO).
- **Outils** : JUnit 5, Mockito.
- **Périmètre** : Couverture des services Spring Boot (ex: `LotServiceTest`, `AlerteServiceTest`).

### 1.2 Tests d'Intégration (API & Base de données)
- **Objectif** : Vérifier que les composants communiquent correctement entre eux (Persistance SQL, réception des messages MQTT via le Broker).
- **Outils** : Spring Boot Test, Testcontainers (pour la BDD SQL et Mosquitto).
- **Périmètre** : Routes API REST (ex: `LotApiIntegrationTest`).

### 1.3 Tests de Recette & End-to-End (UI)
- **Objectif** : Simuler le parcours d'un utilisateur final (Magasinier, Qualiticien) sur l'interface Web pour s'assurer que les données consolidées s'affichent correctement.
- **Outil retenu** : **Selenium** (via Python).

---

## 2. Jeux d'essai et Cas de Test (Recette Fonctionnelle)

La phase de test de l'interface s'appuie sur le jeu d'essai suivant :

| Donnée injectée | Contexte / Contrainte | Résultat Attendu sur l'Interface (Web) |
| :--- | :--- | :--- |
| Lot `LOT-BR-001` | Température actuelle : 29°C, Humidité : 55% | Le lot s'affiche avec le statut **CONFORME** (Pastille verte). Aucune alerte. |
| Lot `LOT-CO-999` | Température actuelle : 38°C (Colombie) | La courbe de température dépasse le seuil. Statut **ALERTE**. Une notification d'alerte rouge est visible. |
| Lot `LOT-EQ-OLD` | Date d'entrée : 15 Mars de l'année N-2 | Le lot dépasse 365 jours de stockage. Une alerte de type "Péremption FIFO" est levée. |

---

## 3. Mise en œuvre et Testabilité (Outil : Selenium)

Pour valider l'affichage des données remontées par les capteurs IoT, nous avons développé un script de test automatisé avec **Selenium WebDriver**.

**Parcours du test (Scénario automatisé) :**
1. Lancement du navigateur (Chrome) en mode fantôme (Headless).
2. Connexion à l'URL du Dashboard central (`frontend-web`).
3. Accès à la page "Alertes".
4. Vérification de la présence d'un élément d'alerte (HTML) correspondant au Lot `LOT-CO-999`.
5. Génération d'un rapport de succès ou d'échec dans la console.

Ce test peut être déclenché localement par les développeurs pour valider la non-régression de l'interface avant toute mise en production.
