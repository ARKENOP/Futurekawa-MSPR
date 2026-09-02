# Dispositif d'alerte — règles, seuils, fréquence, notifications

Le cahier des charges (§III.4) demande que le dispositif d'alerte soit documenté :
règles, seuils, fréquence de vérification et contenu des e-mails. C'est l'objet de ce
document. Il décrit ce qui est **implémenté**, pas une cible.

## 1. Les deux règles d'alerte

| Règle | `typeAlerte` | Déclencheur | Fréquence de vérification |
| --- | --- | --- | --- |
| Conditions de stockage hors plage | `CONDITION_NON_IDEALE` | Chaque mesure reçue par MQTT | À chaque mesure, soit toutes les 5 s avec le prototype IoT actuel |
| Lot dépassant sa durée de stockage | `LOT_TROP_ANCIEN` | Âge du lot > `DUREE_MAX_STOCKAGE_JOURS` (365 par défaut) | Tâche planifiée, en tête d'heure par défaut ; rythme réglable par `PEREMPTION_CRON` (`futurekawa.peremption.cron`) |

Aucune des deux règles n'est câblée pour un pays donné : les seuils, les tolérances et la
durée maximale de stockage viennent du `.env` du backend du pays
(`PaysProperties`), donc ajouter un pays ne demande aucune modification de code.

## 2. Seuils et tolérances

Valeurs imposées par le cahier des charges, portées par le `.env` de chaque déploiement :

| Pays | Température idéale | Humidité idéale | Tolérance |
| --- | --- | --- | --- |
| Brésil | 29 °C | 55 % | ±3 °C / ±2 % |
| Équateur | 31 °C | 60 % | ±3 °C / ±2 % |
| Colombie | 26 °C | 80 % | ±3 °C / ±2 % |

## 3. Niveau de gravité

`MesureService.checkThresholds` compare l'écart à l'idéal :

| Écart | `niveau` |
| --- | --- |
| Dans la tolérance | aucune alerte |
| Hors tolérance | `WARNING` |
| Au-delà de **2 ×** la tolérance | `CRITIQUE` |

Une alerte de péremption est toujours `CRITIQUE`.

> `NiveauAlerte.INFO` existe dans l'énumération partagée mais n'est jamais produit par le
> backend. Il est conservé pour la compatibilité du contrat et l'affichage de fiches
> historiques.

## 4. Déduplication

Tant qu'une alerte `CONDITION_NON_IDEALE` reste `OUVERTE` pour un entrepôt, aucune nouvelle
alerte du même type n'est créée pour cet entrepôt. Sans cela, une dérive prolongée
générerait une alerte toutes les 5 secondes.

**Conséquence à connaître** : c'est le backend du pays qui détient le cycle de vie de
l'alerte. Une fiche traitée uniquement dans Odoo laisserait l'alerte ouverte côté backend et
**muterait définitivement la surveillance de cet entrepôt**. C'est pourquoi le module Odoo
répercute ses décisions vers le backend du pays (`PATCH /api/v1/alertes/{id}`) — voir
§6.

## 5. Notification par e-mail

### Destinataires — gérés dans Odoo

Le cahier des charges demande qu'« un email soit envoyé au responsable d'exploitation du
pays concerné ». **La liste des destinataires est tenue dans Odoo, pas dans le code et pas
dans la configuration des backends.**

Le mécanisme est une **étiquette de contact** : les contacts portant l'étiquette
**« Responsable Qualite FutureKawa »** et possédant une adresse e-mail reçoivent les
alertes. `_notify_quality_team` les résout au moment de l'envoi et les passe en
`recipient_ids`.

Pour ajouter, retirer ou changer un destinataire :

> **Odoo → Contacts** → ouvrir (ou créer) le contact → renseigner son e-mail → ajouter
> l'étiquette **« Responsable Qualite FutureKawa »**. Retirer l'étiquette suffit à ne plus
> le notifier.

Conséquences assumées de ce choix :

- **aucune adresse n'existe dans le code source ni dans un `.env`** — c'est la raison du
  choix : la liste est une donnée d'exploitation, gérée par les métiers dans l'ERP, sans
  redéploiement ni intervention technique ;
- le backend du pays **n'envoie aucune adresse** : il déclenche, Odoo adresse et expédie ;
- la liste est **commune à tous les pays**. Pour router par pays, il suffit de créer une
  étiquette par pays et de dupliquer le modèle d'e-mail — la logique de résolution ne
  change pas. Ce n'est pas fait aujourd'hui.

L'étiquette est livrée par le module (`data/partner_category_data.xml`) : elle existe dès
l'installation, il ne reste qu'à étiqueter les contacts.

### Condition d'envoi

**Toute alerte donne lieu à un e-mail**, sur les deux règles et quel que soit le niveau. Il
n'y a pas de seuil de gravité : le cahier des charges demande une notification dès que les
conditions sortent de la plage acceptable, et une dérive `WARNING` est précisément ce
cas-là.

> Historique utile en soutenance : l'envoi était initialement filtré sur
> `niveau == 'critique'`. Comme le backend n'escalade en `CRITIQUE` qu'au-delà de **deux
> fois** la tolérance, une mesure simplement hors tolérance ne prévenait personne — la
> fiche était créée dans l'ERP, mais aucun e-mail ne partait. Le filtre a été retiré.

### Contenu de l'e-mail

Objet : `[FutureKawa] Alerte <niveau> - <entrepôt> (<référence de fiche>)`

Corps :

- référence de la fiche (`ALT/<année>/<séquence>`), entrepôt, pays, niveau, type
  d'anomalie, lot concerné, date de détection ;
- la description produite par le backend, en français, qui porte les valeurs relevées et
  les valeurs idéales — par exemple :
  `Conditions critiques dans Entrepôt Sud A : température 36,8 °C (idéale 29,0 °C),
  humidité 56,0 % (idéale 55,0 %).` ou
  `Le lot BR-2024-0087 dépasse la durée maximale de stockage : 748 jours écoulés depuis
  son entrée en entrepôt le 15/08/2024 (limite 365 jours).` ;
- une section **« Action attendue »** différente selon le type d'anomalie : vérification
  sur place et lecture des courbes pour une dérive de conditions ; priorisation FIFO de
  l'expédition ou déclassement pour un lot trop ancien ;
- un rappel explicite que la surveillance de l'entrepôt reste suspendue tant que la fiche
  n'est pas traitée.

Le modèle est modifiable sans redéploiement : *Odoo → Paramètres → Technique → E-mail →
Modèles → « Alerte Qualite FutureKawa »*.

L'expéditeur est celui configuré dans le modèle d'e-mail Odoo (`email_from`), aligné sur le
compte du serveur SMTP sortant.

### Robustesse

L'envoi ne peut jamais empêcher l'enregistrement de la fiche : le backend crée ces
enregistrements par l'API, donc toute erreur d'e-mail est interceptée et journalisée. Si
aucun contact étiqueté ne possède d'adresse, la fiche est enregistrée quand même et un
avertissement est écrit dans les journaux Odoo, en nommant l'étiquette à utiliser.

## 6. Chaîne complète

```
Capteur DHT22 ─série─> pont Python ─MQTT─> backend du pays
                                              │
                            ┌─────────────────┴─────────────────┐
                            │                                   │
                    PostgreSQL (mesure,                  Odoo (fiche de
                    alerte, statut du lot)               non-conformité)
                            │                                   │
                    backend central                      e-mail aux contacts
                            │                            étiquetés « Responsable
                       frontend siège                    Qualite FutureKawa »
                                                                │
                            └──── PATCH /api/v1/alertes/{id} ◄───┘
                                  (décision qualité, réarme la détection)
```

## 7. Rejouer une alerte sans capteur

La détection se déclenche à la publication MQTT, donc un scénario se rejoue avec
`mosquitto_pub` :

```bash
# Brésil : 38 °C pour un idéal de 29 ± 3 → hors 2× tolérance → CRITIQUE
mosquitto_pub -h <broker> -t 'futurekawa/BR/entrepot/1/mesures' \
  -m '{"id_capteur":"test-01","temperature_c":38.0,"humidite_pourcent":58.0,"timestamp":1756288000000}'

# Brésil : 33 °C → hors tolérance mais sous 2× → WARNING, notifié aussi
mosquitto_pub -h <broker> -t 'futurekawa/BR/entrepot/1/mesures' \
  -m '{"id_capteur":"test-01","temperature_c":33.0,"humidite_pourcent":56.0,"timestamp":1756288100000}'
```

Pour rejouer une deuxième fois le même type d'alerte sur le même entrepôt, il faut d'abord
clôturer la précédente (interface siège, ou fiche Odoo) : la déduplication du §4 s'applique.
