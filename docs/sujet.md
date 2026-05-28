**CERTIFICATION PROFESSIONNELLE EXPERT EN INFORMATIQUE ET SYSTEME D'INFORMATION RNCP35584** 

**Bloc 4-Concevoir et développer des solutions applicatives métier et spécifiques (mobiles, embarquées et ERP)** 

Cahier des Charges de la MSPR "Conception d'une solution applicative en adéquation avec l'environnement technique étudié" 

**COMPÉTENCES ÉVALUÉES:** 

* Collecter les besoins métiers des utilisateurs en menant des interviews auprès d'eux pour comprendre leurs activités et leurs contraintes métier afin d'étudier les opportunités et la faisabilité technologique d'une solution applicative spécifique ou métier. 


* Concevoir une architecture applicative selon la complexité du système d'information existant de type architecture distribuée, ou micro-service évolutive et tolérante aux pannes 


* Développer une application adéquate selon la stratégie applicative de l'environnement en utilisant un langage de programmation approprié dans le respect du cahier des charges établi afin de répondre aux besoins utilisateurs/directions métiers 


* Développer une solution applicative intégrée en utilisant le paramétrage et le langage de programmation spécifique de l'éditeur dans le respect du cahier des charges établi afin de répondre aux besoins utilisateurs/directions métiers 


* Effectuer les tests de la solution applicative paramétrée ou développée pour identifier les erreurs et dysfonctionnements et établir les plans de correction/d'amélioration avant sa mise en production 


* Appliquer l'intégration continue dans le cadre du développement d'une application en utilisant un outil d'intégration continue afin de vérifier la conformité de la solution et les besoins utilisateurs 


* Vérifier la conformité entre la solution développée ou paramétrée et les fonctionnalités attendues à partir des retours des directions métiers afin de rédiger la documentation et les référentiels orientés utilisateurs 


* Conduire le changement auprès des métiers lors du déploiement d'une solution applicative ou intégrée en mettant en place une démarche de participation, de communication et de formation pour accompagner les utilisateurs à l'intégration du nouvel outil dans leurs habitudes de travail. 



**PHASE 1: PRÉPARATION DE CETTE MISE EN SITUATION PROFESSIONNELLE RECONSTITUÉE** Durée de préparation : 24 heures Mise en œuvre: Travail d'équipe constituée de 4 apprenants-candidats (5 maximum si groupe impair) 

**PHASE 2: PRÉSENTATION ORALE COLLECTIVE + ENTRETIEN COLLECTIF** Objectif: mettre en avant et démontrer que les compétences visées par ce bloc sont bien acquises. 

* Moyen: L'équipe utilise un support de présentation 


* Durée totale par groupe: 50 mn se décomposant comme suit: 


* 20 mn de soutenance orale par l'équipe. 


* 30 mn d'entretien collectif avec le jury (questionnement complémentaire). 



Jury d'évaluation 2 personnes (binôme d'évaluateurs) par jury Ces évaluateurs ne sont pas intervenus durant la période de formation et ne connaissent pas les apprenants à évaluer. 



I. CONTEXTE GLOBAL ET MÉTIER 

FutureKawa est une entreprise internationale spécialisée dans la caféiculture et la logistique de café vert. Elle regroupe plusieurs exploitations caféières implantées en Amérique du Sud, avec une présence opérationnelle dans trois pays: Brésil, Équateur et Colombie. Son métier couvre l'ensemble de la chaîne de valeur, de la graine à la récolte, puis le stockage et l'expédition des grains vers ses clients répartis dans le monde. 

1. Activité et chaîne de valeur 

FutureKawa intervient sur plusieurs étapes clés: 

* Production agricole pilotage de parcelles, gestion des cycles de culture, récolte, premières opérations de tri/séchage. 


* Constitution de lots: chaque récolte est regroupée en lots identifiés (Id unique), associés à un pays, une exploitation, une date et des caractéristiques de qualité. 


* Stockage en entrepôts: les lots sont stockés dans des entrepôts dédiés, parfois situés à proximité des zones de récolte ou de hubs logistiques. Les conditions de stockage (température, humidité) sont critiques pour préserver les arômes et éviter la dégradation. 


* Distribution internationale: FutureKawa vend majoritairement du café vert (non torréfié) et organise l'expédition vers ses clients (containers, transitaires, documents douaniers, traçabilité). 



2. Clients, services et modèle économique 

FutureKawa travaille principalement en B2B. Ses clients sont notamment: 

* des torréfacteurs industriels et artisanaux, 


* des marques de café (grande distribution / premium), 


* des importateurs-distributeurs, 


* ponctuellement des acteurs de l'agroalimentaire (extraits, arômes, blends). 



Son business model repose sur : 

* la vente de lots de café vert (contrats cadres + commandes au fil de l'eau), 


* des engagements de qualité et de traçabilité (origine, conditions de stockage, historique), 


* des services associés: gestion des stocks, réservation de lots, priorisation des expéditions, et reporting pour répondre aux exigences des clients (qualité, conformité, traçabilité). 



3. Organisation et organigramme simplifié 

FutureKawa est structurée de façon "groupe", avec une direction centrale et des entités par pays. Direction Générale (siège) 

* CEO/Direction générale: stratégie, grands comptes, conformité 


* Direction Opérations & Supply Chain: logistique, entrepôts, transport, planification Direction Qualité: contrôle qualité lots, audits, traçabilité, gestion des non-conformités Direction Finance & Administration: achats, contrats, facturation, gestion fournisseurs Direction Commerciale: relation clients, prévisions de vente, gestion catalogue lots Direction SI / Informatique: applications, données, infrastructures, sécurité 



Entités par pays (Brésil / Équateur / Colombie) 

* Responsable d'exploitation: production + stockage + expédition locale Responsable entrepôt: réception, stockage, inventaires, préparation lots Référent qualité local: contrôles, alertes qualité, procédures 


* Correspondant Sl local: relais terrain, matériel, support de niveau 1 



4. Enjeux métier actuels: qualité, traçabilité, pertes et conformité 

L'entreprise connaît plusieurs irritants opérationnels: 

* Les conditions de stockage (température/humidité) sont variables selon les entrepôts, avec des conséquences sur la qualité. 


* Les suivis sont souvent semi-manuel (relevés ponctuels, tableaux, outils hétérogènes), rendant difficile l'auditabilité. 


* Les équipes doivent respecter une logique de distribution FIFO: expédier en priorité les lots les plus anciens, mais la visibilité n'est pas toujours fiable. 


* Les clients demandent des preuves: historique des conditions de stockage et gestion proactive des risques (non-conformités). 



Ces enjeux impactent directement: 

* le taux de réclamation, 


* les coûts de pertes (altération, déclassement), 


* la confiance client et la valorisation des lots premium. 


5. Le système d'information et le service informatique 

FutureKawa dispose d'un SI "hybride", composé d'outils historiques et de solutions récentes: 

* Un ERP (gestion achats/ventes/stock/compta) utilisé au siège, avec des adaptations locales. Des outils opérationnels (tableurs, applications internes légères) côté entrepôts. 


* Une volonté d'industrialisation: traçabilité, données, supervision, automatisation. 



Le service informatique est organisé en petite équipe transverse, avec une forte orientation "produit": DSI/Responsable SI: gouvernance, sécurité, priorisation des projets, relation métiers. 

* Équipe Applications (2-3 pers.): développement interne, intégrations (API), gestion ERP et outils métiers. 


* Équipe Data/BI (1-2 pers.) reporting, tableaux de bord, qualité des données, indicateurs supply/qualité. 


* Équipe Infra/DevOps (1-2 pers.): conteneurisation, déploiements, supervision, CI/CD, support N2. 


* Support utilisateurs: assuré par l'infra/DevOps et des correspondants locaux (N1). 



Le Sl est contraint par: 

* des conditions terrain (réseau variable, matériel limité en entrepôts), 


* la nécessité de solutions robustes et maintenables, 


* une logique de déploiement multi-sites/multi-pays, 


* des exigences croissantes sur la sécurité et la traçabilité. 



6. Ambition à 12-18 mois 

FutureKawa souhaite standardiser son suivi de stockage et créer une base solide pour une "phase 2": automatisation des entrepôts (chauffage, humidification, aération pilotée par capteurs), avec une approche progressive afin de sécuriser l'adoption par les équipes locales. 

## II. 

CAHIER DES CHARGES 

FutureKawa souhaite mettre en place une solution applicative de suivi des stocks et des conditions de stockage des grains de café, multi-pays, intégrant un dispositif loT. Le projet est commandité par la Direction des Opérations et la Direction Qualité de FutureKawa, avec le soutien de la Direction Sl. 

Les bénéficiaires directs sont : 

* les responsables d'exploitation (Brésil / Équateur / Colombie), 


* les responsables d'entrepôt (réception, stockage, expédition), 


* les équipes Qualité (traçabilité, conformité), 


* les équipes Supply Chain (rotation des stocks, priorités d'expédition), 


* le siège (pilotage, consolidation, reporting global). 



Finalité et objectifs (niveau "macro") 

FutureKawa souhaite disposer d'un outil unique et fiable permettant de: 

* centraliser le suivi des stocks par pays et par entrepôt, 


* garantir la traçabilité des lots depuis leur entrée en stockage, 


* surveiller les conditions de conservation (température / humidité) via des relevés automatisés, détecter et signaler les situations à risque (dérives de conditions, lots trop anciens), 


* fournir une base technique et fonctionnelle préparant une évolution vers l'automatisation des équipements d'entrepôt (chauffage, humidification, aération). 



Cadre de réalisation et hypothèses 

* La solution doit être déployable dans un environnement multi-sites / multi-pays, avec une approche standardisée mais adaptable aux spécificités locales. 


* Le système doit fonctionner avec des contraintes "terrain": matériel simple, conditions d'exploitation variables, et besoin de robustesse. 


* Le projet est réalisé sous forme de prototype avancé il doit démontrer la faisabilité et fournir une solution suffisamment aboutie pour être évaluée et présentée à un jury, et servir de base à une industrialisation. 



Exigences globales attendues 

Sans entrer dans les détails fonctionnels (qui seront précisés en partie III), le cahier des charges impose: 

* une solution applicative (backend + frontend) permettant consultation et pilotage, 


* une intégration loT s'appuyant sur un protocole adapté (MQTT), 


* une persistance fiable des données (base SQL), 


* une architecture cohérente avec un SI distribué (pays siège), 


* un niveau de qualité "projet" documentation, tests, intégration continue, et préparation au changement. 



## III. 

BESOINS EXPRIMÉS PAR LE CLIENT 

FutureKawa exprime le besoin d'une solution applicative complète permettant de suivre les lots stockés et de surveiller automatiquement les conditions de conservation dans les entrepôts des trois pays (Brésil, Équateur, Colombie), avec une consolidation centrale au siège. 

1. Gestion des exploitations / entrepôts et des lots (stocks) 

Objectif: assurer une traçabilité fiable des lots stockés et faciliter la rotation FIFO. Besoins détaillés: 

* La solution doit permettre de gérer plusieurs exploitations, au minimum 3 pays (Brésil, Équateur, Colombie) et leurs entrepôts. 


* Chaque exploitation produit des lots de grains identifiés par un Id unique. 


* Pour chaque lot, l'entreprise souhaite mémoriser au minimum: I'ld du lot, le pays/exploitation / entrepôt, la date de stockage (entrée en entrepôt), un statut (ex: conforme/en alerte / périmé, selon vos choix). 


* Les lots doivent pouvoir être consultés et triés par date de stockage afin d'appliquer une logique FIFO : expédier en priorité les lots les plus anciens. 


* Le siège doit pouvoir consulter l'état des stocks de chaque pays depuis une interface centralisée. 



2. Surveillance loT: température et humidité par entrepôt 

Objectif: instrumenter les entrepôts afin de remonter automatiquement les conditions de stockage. Besoins détaillés : Chaque pays doit disposer d'un module IoT (microcontrôleur + capteur température / humidité) réalisant des relevés automatiques de : 

* température (°C) 


* humidité (%) 



Les relevés doivent être transmis via un broker MQTT (local au pays). Les données doivent être persistées (stockées) dans une base de données SQL. Les conditions idéales attendues diffèrent selon le pays: 

* Brésil: 29°C/55% humidité 


* Équateur: 31°C/60% humidité 


* Colombie: 26°C/80% humidité 



Tolérance acceptable: 

* ±3°C pour la température 


* ±2% pour l'humidité 



L'entreprise souhaite pouvoir consulter un historique des mesures afin de justifier la traçabilité et d'analyser les dérives. 



3. Application Web: consultation des stocks et visualisation des historiques 

Objectif: fournir un outil unique aux équipes locales et au siège. Besoins détaillés: La solution doit proposer une interface Web permettant: 

* de sélectionner une exploitation / un pays, 


* d'afficher la liste des lots triés par date de stockage, 


* de sélectionner un lot, 


* d'afficher les courbes de température et d'humidité enregistrées depuis son stockage. 



L'entreprise attend une interface: 

* lisible pour des utilisateurs "terrain" (entrepôt), 


* exploitable par le siège (supervision/consolidation), 


* avec un accès rapide aux alertes et au statut des lots. 



4. Alertes automatiques: qualité et péremption 

Objectif: détecter rapidement les risques (qualité/stocks) et déclencher une action. Besoins détaillés: La solution doit lever une alerte automatique dans deux cas: 

* Conditions non idéales: température/humidité hors plage acceptable au regard du pays concerné. 


* Lot trop ancien: un lot dépassant 1 an (365 jours) de stockage. 



En cas d'alerte, un email doit être envoyé au responsable d'exploitation du pays concerné. Le dispositif d'alerte doit être documenté (règles, seuils, fréquence de vérification, contenu des emails). 

5. Architecture distribuée: pays (local) + siège (central) 

Objectif: refléter l'organisation multi-pays et préparer une montée en charge. Besoins détaillés: Chaque pays doit disposer d'un backend local conteneurisé comprenant: 

* une base SQL, 


* un broker MQTT local, 


* une API REST permettant: d'enregistrer de nouveaux lots, d'exposer l'état des stocks et les mesures au siège, 


* un dispositif d'alerting pour email & règles. 



Le siège doit disposer d'un backend central permettant: 

* de requêter les backends des différents pays via des routes d'API, 


* d'alimenter le frontend en informations consolidées (stocks, mesures, alertes). 



Le siège héberge également le frontend Web. Les choix d'architecture (microservices, découpage, résilience) doivent être justifiés. 

6. Préparation au changement: vers l'automatisation des entrepôts 

Objectif: préparer la "phase 2" et accompagner les métiers. Besoins détaillés : FutureKawa envisage de mettre en place, dans chaque entrepôt, des équipements automatisés : chauffage, humidification, aération, pilotés par les relevés température/humidité. Le client étant encore imprécis, l'entreprise demande: 

* un prototype de schéma de fonctionnement (logique capteurs + décision actionneurs + sécurités), 


* un questionnaire destiné à une prochaine interview pour préciser: les besoins métiers, les contraintes de sécurité, les limites d'automatisation acceptables, les modalités de maintenance et d'exploitation, les priorités de déploiement. 



## IV. 

LIVRABLES 

FutureKawa attend une livraison structurée permettant de démontrer la faisabilité, d'évaluer la qualité de la solution, et de préparer une mise en production progressive. Les livrables devront permettre au jury d'apprécier le travail de conception, de développement, de tests, d'industrialisation (CI/CD) et de préparation au changement. 

1. Un backend d'un pays "exemple", conteneurisé 

L'entreprise souhaite disposer d'un backend local complet pour un pays (au choix des apprenants) comprenant: 

* une API REST pour gérer les lots et exposer les mesures, 


* une base de données SQL, 


* un broker MQTT local, 


* un mécanisme de levée d'alertes et d'envoi d'emails, 


* une exécution conteneurisée (ex. Docker / Docker Compose), reproductible sur un poste de démonstration. 



Attendu un ensemble démarrable via une commande simple (ex docker compose up) + documentation de lancement. 

2. Un backend central "siège" et un Frontend Web (siège) 

FutureKawa attend la solution "siège" composée de : 

* un backend central capable d'interroger les backends pays via des routes d'API, afin de consolider: l'état des stocks, les mesures historiques, les alertes, 


* un frontend Web permettant: la sélection d'une exploitation/pays, l'affichage des lots triés par date de stockage, la consultation d'un lot et de ses courbes température/humidité, l'accès aux informations d'alerte et aux statuts. 



Attendu: solution utilisable en démonstration, cohérente et documentée. 



3. Un prototype fonctionnel du module loT 

L'équipe devra livrer un prototype opérationnel basé sur : 

* un microcontrôleur, 


* un capteur capable de: relever température et humidité, publier les données vers le broker via MQTT, 


* démontrer la réception et l'exploitation des données par le backend (persistées et consultables). 



Attendu: preuve de fonctionnement (démo live ou scénario reproductible). 

4. Documentation argumentée (dossier technique) 

Un dossier synthétique et structuré est exigé, incluant:
 
4.1. Architecture globale 

* description de l'architecture pays + siège, des flux et composants, 


* justification des choix technologiques et du découpage, 


* éléments de robustesse (tolérance pannes, reprise, logs, supervision - selon choix). 



4.2. Conception du module loT 

* schéma de câblage, choix matériel, limites/risques, 


* protocole MQTT (topics, formats payload, fréquence d'envoi), 


* stratégie de reconnexion / gestion des erreurs (selon vos choix). 



4.3. Plans de tests détaillés 

* stratégie et typologie (unitaires / intégration/API/UI/end-to-end), 


* cas de test, données de test, critères de réussite, 


* gestion des anomalies (constat, correction, re-test). 



5. Pipelines de CI/CD automatisées (Jenkins) 

L'entreprise exige des pipelines permettant d'industrialiser le projet : build, exécution automatisée des tests, vérification qualité (selon outils retenus), packaging (ex: images Docker / artefacts), mise à disposition d'artefacts exploitables pour la démo. Attendu: Jenkinsfile ou configuration Jenkins documentée + preuve d'exécution. 

6. Tests lançables manuellement 

En complément de la Cl, des tests doivent être exécutable manuellement: 

* commande(s) simple(s), 


* documentation claire (pré-requis, variables, jeux de données), 


* résultats lisibles (logs/rapport). 



7. Code source versionné sur un repository Git 

FutureKawa exige un dépôt Git comprenant: 

* l'ensemble du code (backend pays, central, frontend, IoT, scripts), 


* la structure de branches et commits cohérente, 


* un README de prise en main, 


* la documentation et scripts de lancement, 


* éventuellement des issues / backlog (selon votre organisation). 



8. Documentation utilisateur (orientation métier) 

Une documentation orientée "utilisateurs métiers" doit être fournie, comprenant: 

* prise en main de l'interface Web (parcours, étapes), 


* création/consultation des lots, 


* lecture des courbes (temp/humidité), 


* compréhension des alertes et actions attendues, 


* résolution des problèmes simples/FAQ. 



9. Prototype de schéma: automatisation chauffage / aération / humidification 

FutureKawa attend un schéma de principe illustrant un futur fonctionnement automatisé : 

* capteurs traitement/décision actionneurs, 


* cas nominal et cas dégradé, 


* sécurités (seuils, arrêt d'urgence logique, manuel/auto, etc.), 


* point d'intégration avec la solution lot existante. 



10. Questionnaire pour l'interview de cadrage "phase 2" 

Un questionnaire structuré est attendu pour la prochaine réunion client, afin de préciser: objectifs métier de l'automatisation, 

* contraintes (sécurité, maintenance, coûts, responsabilités), 


* tolérances, modes manuels/automatiques, 


* priorités de déploiement et indicateurs de réussite, 


* risques et scénarios d'incident. 



En complément des livrables, l'équipe projet devra préparer un support de présentation destiné à la soutenance finale devant le client (public technique). Ce support devra synthétiser les principaux éléments du travail réalisé. Il est important de souligner que l'évaluation de cette MSPR repose sur la combinaison des trois éléments suivants: 

* la qualité du travail réalisé au cours du projet, 


* la pertinence et l'exhaustivité des livrables remis 


* et la capacité de l'équipe à présenter, justifier et valoriser ce travail lors de la soutenance orale. 



Les équipes devront donc s'assurer que la soutenance reflète bien l'ensemble des compétences attendues (cf. page 1), en démontrant à la fois la maîtrise technique et la capacité à communiquer efficacement auprès d'un client professionnel. 

V. RESSOURCES FOURNIES 

Pour réaliser le projet, FutureKawa (via l'organisme de formation) met à disposition des équipes un ensemble de ressources matérielles, contraintes techniques et préconisations permettant de cadrer l'environnement de réalisation et de démonstration. 

1. Ressources matériels 

Chaque équipe dispose au minimum de: 

* 1 microcontrôleur 


* 1 breadboard 


* 1 capteur température/humidité 


* câblage de prototypage (selon stocks du campus) 



Information logistique: les références exactes du matériel seront communiquées par message privé aux équipes (selon vos procédures internes). 

2. Webographie 

IoT/ESP32/Capteurs 

* Arduino-ESP32 (core officiel Espressif pour Arduino) - Espressif Systems - Documentation d'installation, API, exemples [https://docs.espressif.com/projects/arduino-esp32/](https://docs.espressif.com/projects/arduino-esp32/) 


* MicroPython - Documentation - MicroPython Documentation générale + modules [https://docs.micropython.org/](https://docs.micropython.org/) 


* MicroPython - Quick reference ESP32 - MicroPython - Référence rapide ESP32 (GPIO, Wi-Fi, etc.) [https://docs.micropython.org/en/latest/esp32/quickref.html](https://docs.micropython.org/en/latest/esp32/quickref.html) 


* Adafruit Learn - DHT11/DHT22-Adafruit - Guides capteurs température/humidité (câblage, code) [https://learn.adafruit.com/dht](https://learn.adafruit.com/dht) 



MQTT/Broker / Déploiement 

* MQTT v5.0 (spécification) - OASIS - Spécification du protocole MQTT-[https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html) 


* Mosquitto (broker MQTT) - Eclipse Foundation [https://mosquitto.org/documentation/](https://mosquitto.org/documentation/) Documentation du broker, configuration 


* Docker Hub Image "eclipse-mosquitto" Docker image Mosquitto (conteneur officiel) - Do [https://hub.docker.com//eclipse-mosquitto](https://hub.docker.com//eclipse-mosquitto) 



Node-RED (alerting, orchestration) 

* Node-RED Documentation [https://nodered.org/docs/](https://nodered.org/docs/) OpenJS Foundation 


* Node-RED Cookbook - OpenJS Foundation [https://cookbook.nodered.org/](https://cookbook.nodered.org/) Documentation officielle Recettes (MQTT, JSON, patterns de flux) 



Conteneurisation / Docker Compose 

* Docker Documentation - Docker Documentation officielle- [https://docs.docker.com/](https://docs.docker.com/) 


* Docker Compose (référence) [https://docs.docker.com/compose/](https://docs.docker.com/compose/) Docker Spécification et utilisation de Compose 



CI/CD -Jenkins 

* Jenkins Documentation (User Handbook) [https://www.jenkins.io/doc/](https://www.jenkins.io/doc/) Jenkins Documentation officielle 


* Jenkins Pipeline - Jenkins - Concepts + Jenkinsfile- [https://www.jenkins.io/doc/book/pipeline/](https://www.jenkins.io/doc/book/pipeline/) 



API/Documentation/Contrats 

* OpenAPI Initiative Linux Foundation [https://www.openapis.org/](https://www.openapis.org/) OpenAPI Specification Présentation du standard et écosystème 


* OpenAPI Initiative [https://spec.openapis.org/oas/latest.html](https://spec.openapis.org/oas/latest.html) Spécification officielle 


* Swagger (OpenAPI tools) SmartBear Outils de documentation/visualisation - [https://swagger.io/](https://swagger.io/) 


* PostgreSQL Documentation PostgreSQL Global Development Group Documentation officielle [https://www.postgresql.org/docs/](https://www.postgresql.org/docs/) 


* MariaDB Documentation MariaDB Foundation [https://mariadb.com/kb/en/documentation/](https://mariadb.com/kb/en/documentation/) Documentation officielle 



Frontend & dataviz 

* React Documentation Meta-Documentation officielle [https://react.dev/](https://react.dev/) 


* Chart.js Documentation Chart.js Contributors Graphiques (courbes, séries temporelles) [https://www.chartjs.org/docs/latest/](https://www.chartjs.org/docs/latest/) 



Sécurité applicative 

* OWASP API Security Top 10 OWASP Foundation Risques majeurs sur APIs + recommandations [https://owasp.org/www-project-api-security/](https://owasp.org/www-project-api-security/) 



3. Assistance et périmètre 

Dans le cadre de ce projet pédagogique, l'équipe projet n'aura aucun contact direct avec le client. Le cahier des charges constitue la seule expression officielle du besoin. Toute demande de clarification devra être traitée avec l'encadrant pédagogique, jouant le rôle du client. 