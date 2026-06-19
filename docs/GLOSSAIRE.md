# Glossaire technique et métier FutureKawa

Ce glossaire fixe le vocabulaire canonique du projet. Il doit servir de référence pour nommer les futures variables, classes, tables, endpoints, messages MQTT et éléments de documentation.

## Principes de nommage

- Employer en priorité les termes de ce glossaire, sans synonymes concurrents.
- Utiliser le singulier pour les entités métier et le pluriel pour les collections.
- Éviter les abréviations non définies dans ce document.
- Préférer des noms explicites et stables, alignés sur le métier de FutureKawa.
- Dans le code, conserver la forme sans accents si nécessaire, mais garder le terme métier officiel dans la documentation.

## Vocabulaire métier canonique

| Terme canonique | Définition | Usage recommandé |
| --- | --- | --- |
| FutureKawa | Entreprise fictive du projet, spécialisée dans la caféiculture et la logistique de café vert. | Contexte global du projet |
| Pays | Zone géographique d’exploitation et de déploiement de la solution. Les pays cibles sont le Brésil, l’Équateur et la Colombie. | Regroupement principal des entités locales |
| Exploitation | Unité opérationnelle locale rattachée à un pays, couvrant la production, le stockage et l’expédition. | Entité métier de niveau terrain |
| Entrepôt | Site de stockage où les lots de café vert sont réceptionnés, conservés et préparés pour expédition. | Entité centrale pour le suivi des stocks et des mesures |
| Lot | Unité de stock identifiée de manière unique, issue d’une récolte et stockée dans un entrepôt. | Entité principale du suivi de stock |
| Stock | Ensemble des lots présents dans un pays, une exploitation ou un entrepôt à un instant donné. | Vue de consultation et de pilotage |
| Traçabilité | Capacité à reconstituer l’historique d’un lot, d’un entrepôt et des conditions de conservation associées. | Exigence métier transverse |
| FIFO | Logique de rotation consistant à expédier en priorité les lots les plus anciens. | Règle de gestion des stocks |
| Péremption | Dépassement de la durée maximale de stockage retenue pour le projet, fixée à 365 jours. | Déclencheur d’alerte |
| Condition de stockage | Valeur de température et d’humidité mesurée dans un entrepôt à un instant donné. | Surveillance IoT |
| Conditions idéales | Valeurs cibles de température et d’humidité propres à chaque pays. | Référence de contrôle |
| Tolérance | Écart maximal accepté par rapport aux conditions idéales. | Paramètre de règle |
| Alerte | Signal automatique généré en cas de condition non conforme ou de lot trop ancien. | Entité de suivi et de notification |
| Responsable d’exploitation | Référent local chargé du suivi opérationnel d’un pays. | Destinataire principal des emails d’alerte |
| Responsable d’entrepôt | Référent local chargé de la réception, du stockage et de l’expédition dans un entrepôt. | Utilisateur métier local |
| Référent qualité local | Interlocuteur local chargé des contrôles qualité et de la traçabilité. | Utilisateur métier local |
| Siège | Niveau central de pilotage de FutureKawa. | Consultation consolidée et supervision |

## Entités métier principales

### Pays

Le pays est l’unité de référence des seuils de stockage, des données IoT et de la consolidation centrale.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idPays | Identifiant | Identifiant unique du pays |
| nomPays | Chaîne de caractères | Nom du pays |
| codePays | Chaîne de caractères | Code court stable, utile pour les échanges et les routes d’API |
| temperatureIdealeC | Nombre | Température cible du pays en degrés Celsius |
| humiditeIdealePourcent | Nombre | Humidité cible du pays en pourcentage |
| toleranceTemperatureC | Nombre | Écart autorisé sur la température |
| toleranceHumiditePourcent | Nombre | Écart autorisé sur l’humidité |
| estActif | Booléen | Indique si le pays est pris en charge par la solution |

Relations : un pays regroupe plusieurs exploitations, plusieurs entrepôts, plusieurs lots et plusieurs alertes.

### Exploitation

L’exploitation représente l’unité locale de production et de pilotage rattachée à un pays.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idExploitation | Identifiant | Identifiant unique de l’exploitation |
| nomExploitation | Chaîne de caractères | Nom de l’exploitation |
| pays | Pays | Pays de rattachement |
| localisation | Chaîne de caractères | Localisation textuelle ou géographique |
| responsableExploitation | Responsable d’exploitation | Référent local |
| estActive | Booléen | Indique si l’exploitation est en service |

Relations : une exploitation possède plusieurs entrepôts et plusieurs lots.

### Entrepôt

L’entrepôt est le lieu de stockage opérationnel instrumenté par les capteurs IoT.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idEntrepot | Identifiant | Identifiant unique de l’entrepôt |
| nomEntrepot | Chaîne de caractères | Nom fonctionnel de l’entrepôt |
| exploitation | Exploitation | Exploitation de rattachement |
| pays | Pays | Pays de rattachement |
| localisation | Chaîne de caractères | Adresse ou zone de stockage |
| capaciteMax | Nombre | Capacité maximale de stockage si elle est connue |
| statutEntrepot | Statut | État courant de l’entrepôt |
| temperatureActuelleC | Nombre | Température courante mesurée |
| humiditeActuellePourcent | Nombre | Humidité courante mesurée |

Relations : un entrepôt contient plusieurs lots, produit plusieurs mesures et peut être associé à plusieurs alertes.

### Lot

Le lot est l’unité centrale de traçabilité et de consultation des stocks.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idLot | Identifiant | Identifiant unique du lot |
| referenceLot | Chaîne de caractères | Référence lisible du lot |
| exploitation | Exploitation | Origine opérationnelle du lot |
| pays | Pays | Pays d’origine ou de stockage |
| entrepot | Entrepôt | Entrepôt de stockage courant |
| dateEntreeStockage | Date/heure | Date d’entrée en entrepôt |
| dateRecolte | Date | Date de récolte si elle est connue |
| statutLot | StatutLot | État métier du lot, par exemple conforme, en alerte ou périmé |
| qualiteLot | Chaîne de caractères | Niveau ou appréciation qualité si elle est suivie |
| ancienneteJours | Nombre calculé | Durée de stockage exprimée en jours |

Relations : un lot appartient à une exploitation, est stocké dans un entrepôt et peut déclencher des alertes.

### Mesure de stockage

La mesure de stockage est une lecture IoT enregistrée pour un entrepôt à un instant donné.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idMesure | Identifiant | Identifiant unique de la mesure |
| entrepot | Entrepôt | Entrepôt concerné par la mesure |
| pays | Pays | Pays du relevé |
| dateHeureMesure | Date/heure | Horodatage précis du relevé |
| temperatureC | Nombre | Température mesurée |
| humiditePourcent | Nombre | Humidité mesurée |
| sourceMesure | SourceIoT | Origine technique de la donnée |
| topicMQTT | Chaîne de caractères | Sujet MQTT utilisé pour la publication |

Relations : un entrepôt possède plusieurs mesures de stockage dans le temps.

### Alerte

L’alerte matérialise une anomalie de stockage, de conservation ou de péremption.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idAlerte | Identifiant | Identifiant unique de l’alerte |
| typeAlerte | TypeAlerte | Nature de l’alerte, par exemple condition non idéale ou lot trop ancien |
| niveauAlerte | NiveauAlerte | Gravité métier |
| statutAlerte | StatutAlerte | État courant, par exemple ouverte, notifiée ou clôturée |
| messageAlerte | Chaîne de caractères | Résumé lisible du problème |
| dateCreation | Date/heure | Date de génération de l’alerte |
| dateCloture | Date/heure | Date de résolution si elle existe |
| pays | Pays | Pays concerné |
| exploitation | Exploitation | Exploitation concernée si applicable |
| entrepot | Entrepôt | Entrepôt concerné si applicable |
| lot | Lot | Lot concerné si l’alerte porte sur un stock |
| destinataireEmail | Chaîne de caractères | Adresse email du responsable à prévenir |

Relations : une alerte peut concerner un pays, une exploitation, un entrepôt ou un lot.

### Règle d’alerte

La règle d’alerte formalise les seuils et les conditions de déclenchement.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idRegleAlerte | Identifiant | Identifiant unique de la règle |
| typeRegle | TypeRegleAlerte | Règle de conditions ou règle de péremption |
| pays | Pays | Pays auquel s’applique la règle |
| temperatureMinC | Nombre | Seuil minimal acceptable |
| temperatureMaxC | Nombre | Seuil maximal acceptable |
| humiditeMinPourcent | Nombre | Seuil minimal acceptable |
| humiditeMaxPourcent | Nombre | Seuil maximal acceptable |
| dureeMaxStockageJours | Nombre | Durée maximale de stockage autorisée |
| frequenceVerification | Chaîne de caractères | Fréquence de contrôle, par exemple périodique |
| estActive | Booléen | Indique si la règle est en service |

Relations : une règle s’applique à un pays et peut produire des alertes.

### Utilisateur métier

L’utilisateur métier est une personne qui consulte, pilote ou contrôle la solution.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idUtilisateur | Identifiant | Identifiant unique |
| nom | Chaîne de caractères | Nom de famille |
| prenom | Chaîne de caractères | Prénom |
| email | Chaîne de caractères | Adresse de contact |
| roleUtilisateur | RoleUtilisateur | Fonction métier ou technique |
| pays | Pays | Pays de rattachement si applicable |
| exploitation | Exploitation | Exploitation de rattachement si applicable |
| estActif | Booléen | Indique si le compte est actif |

## Entités techniques et IoT

### Microcontrôleur

Le microcontrôleur est le composant embarqué chargé de lire les capteurs et de publier les mesures.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idMicrocontroleur | Identifiant | Identifiant du matériel |
| referenceMaterielle | Chaîne de caractères | Modèle ou référence |
| adresseMac | Chaîne de caractères | Identifiant réseau si disponible |
| wifiState | EtatConnexion | État de la connectivité |
| versionFirmware | Chaîne de caractères | Version du programme embarqué |
| pays | Pays | Pays de déploiement |
| entrepot | Entrepôt | Entrepôt piloté |

### Capteur IoT

Le capteur IoT fournit les valeurs de température et d’humidité.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idCapteur | Identifiant | Identifiant du capteur |
| typeCapteur | TypeCapteur | Type de capteur, par exemple température/humidité |
| referenceMaterielle | Chaîne de caractères | Référence matérielle |
| microcontroleur | Microcontrôleur | Support embarqué |
| entrepot | Entrepôt | Entrepôt surveillé |
| estFonctionnel | Booléen | Indique l’état de fonctionnement |

### Actionneur

L’actionneur est un composant futur destiné à agir sur le chauffage, l’humidification ou l’aération.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| idActionneur | Identifiant | Identifiant de l’actionneur |
| typeActionneur | TypeActionneur | Chauffage, humidification ou aération |
| modeFonctionnement | ModeFonctionnement | Manuel ou automatique |
| etatActionneur | EtatActionneur | Actif, inactif, en défaut |
| entrepot | Entrepôt | Entrepôt concerné |

### Message MQTT

Le message MQTT est le format de transport utilisé pour remonter les mesures depuis le microcontrôleur.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| topicMQTT | Chaîne de caractères | Sujet de publication |
| payloadMQTT | Objet JSON | Données transportées |
| qosMQTT | Nombre | Niveau de qualité de service |
| retainMQTT | Booléen | Indique si le message doit être conservé |
| dateHeureEmission | Date/heure | Horodatage d’émission |

### Payload de mesure

Le payload de mesure est le contenu fonctionnel publié dans le message MQTT.

| Attribut principal | Type métier | Rôle |
| --- | --- | --- |
| pays | Pays | Pays d’origine |
| entrepot | Entrepôt | Entrepôt concerné |
| temperatureC | Nombre | Température relevée |
| humiditePourcent | Nombre | Humidité relevée |
| dateHeureMesure | Date/heure | Horodatage du relevé |
| idCapteur | Identifiant | Référence du capteur émetteur |

## Vocabulaire applicatif et architecture

| Terme canonique | Définition |
| --- | --- |
| Backend local | Service applicatif déployé dans un pays pour enregistrer les lots, recevoir les mesures MQTT et gérer les alertes locales. |
| Backend central | Service applicatif du siège qui interroge les backends locaux et consolide les données. |
| Frontend Web | Interface de consultation utilisée par les équipes locales et le siège. |
| API REST | Interface HTTP exposant les opérations métier. |
| Base SQL | Base de données relationnelle utilisée pour persister les lots, les mesures et les alertes. |
| Broker MQTT | Serveur de messagerie MQTT recevant les publications des microcontrôleurs. |
| Conteneur | Unité d’exécution isolée utilisée pour déployer une brique du système. |
| Docker Compose | Mécanisme d’orchestration local des conteneurs du prototype. |
| CI/CD | Chaîne d’intégration et de livraison continue automatisant les vérifications, les tests et le packaging. |
| Jenkins | Outil d’intégration continue retenu pour automatiser les pipelines. |
| Documentation utilisateur | Documentation orientée métier expliquant l’usage de l’application. |
| Documentation technique | Documentation destinée à expliciter l’architecture, les choix techniques et les tests. |

## Vocabulaire fonctionnel des statuts et des règles

### StatutLot

| Valeur canonique | Définition |
| --- | --- |
| conforme | Le lot est stocké dans des conditions acceptables. |
| en_alerte | Le lot fait l’objet d’une vigilance ou d’une anomalie. |
| perime | Le lot a dépassé la durée maximale de stockage. |

### StatutAlerte

| Valeur canonique | Définition |
| --- | --- |
| ouverte | L’alerte est créée et reste à traiter. |
| notifiee | L’alerte a été transmise au destinataire. |
| cloturee | L’alerte a été traitée et fermée. |

### NiveauAlerte

| Valeur canonique | Définition |
| --- | --- |
| info | Information sans impact critique. |
| warning | Dérive à surveiller. |
| critique | Situation nécessitant une action rapide. |

### TypeAlerte

| Valeur canonique | Définition |
| --- | --- |
| condition_non_ideale | Température ou humidité hors tolérance. |
| lot_trop_ancien | Stockage supérieur à 365 jours. |

### ModeFonctionnement

| Valeur canonique | Définition |
| --- | --- |
| manuel | L’opérateur pilote le système. |
| automatique | La logique métier décide de l’action. |

### EtatConnexion

| Valeur canonique | Définition |
| --- | --- |
| connecte | Le composant embarqué communique correctement. |
| deconnecte | Le composant n’a plus de liaison réseau. |
| en_reconnexion | Une tentative de reprise est en cours. |

### TypeRegleAlerte

| Valeur canonique | Définition |
| --- | --- |
| conditions | Règle liée à la température et à l’humidité. |
| peremption | Règle liée à la durée de stockage. |

## Vocabulaire de consultation et de pilotage

| Terme canonique | Définition |
| --- | --- |
| Vue consolidation | Écran ou agrégat présentant la situation de plusieurs pays ou entrepôts depuis le siège. |
| Vue FIFO | Vue triée par date d’entrée en stockage pour servir la rotation des lots. |
| Historique des mesures | Série temporelle des relevés de température et d’humidité. |
| Courbe de température | Représentation graphique de la température dans le temps. |
| Courbe d’humidité | Représentation graphique de l’humidité dans le temps. |
| Dérive | Écart progressif ou ponctuel par rapport aux conditions idéales. |
| Requête centralisée | Consultation effectuée par le siège sur les backends locaux. |

## Relations métier de référence

- Un pays regroupe plusieurs exploitations.
- Une exploitation regroupe plusieurs entrepôts.
- Un entrepôt contient plusieurs lots.
- Un entrepôt produit plusieurs mesures de stockage.
- Un lot appartient à une exploitation et est stocké dans un entrepôt.
- Une alerte peut concerner un pays, une exploitation, un entrepôt ou un lot.
- Un utilisateur métier agit sur un pays ou une exploitation selon son rôle.

## Termes à privilégier dans le projet

- Utiliser lot plutôt que batch.
- Utiliser entrepôt plutôt que warehouse.
- Utiliser exploitation plutôt que site de production.
- Utiliser mesure de stockage plutôt que simple mesure.
- Utiliser alerte plutôt que incident lorsque le besoin porte sur la notification métier.
- Utiliser siège pour la consolidation centrale.
- Utiliser conditions idéales et tolérance plutôt que valeurs cibles vagues.
