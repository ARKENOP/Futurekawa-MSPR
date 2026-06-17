# Backend Central — Contrat d'API (ce qu'il doit générer)

> Document de référence pour l'implémentation du **backend central**.
> Il décrit **exactement** la forme JSON que le central doit exposer au `frontend-web`.
> La source de vérité des champs est l'ensemble des `*Response` du `backend-local`
> (`backend-local/src/main/java/com/futurekawa/backendlocal/dto/response/`).
> Le miroir TypeScript correspondant vit dans `frontend-web/src/types/api.ts` — **ces deux
> fichiers doivent rester synchronisés.**

---

## 1. Rôle du backend central

- **Agrège** les N backends locaux (un par pays : Brésil `BR`, Équateur `EC`, Colombie `CO`).
  Chaque local expose `GET {baseLocal}/api/v1/...` sans authentification applicative (service M2M).
- **Regroupe par pays** les ressources et **réexpose** une API REST unique au frontend sous `/api/v1`.
- **Sécurité** : valide le JWT **Keycloak** (OIDC) sur chaque requête entrante du frontend.
- **Résilience** : un **circuit breaker Resilience4j par backend local** ; un pays en panne
  ne doit jamais faire échouer toute la réponse (voir §5).

### Pourquoi le regroupement par pays est obligatoire

Chaque backend local possède sa **propre base PostgreSQL** avec des IDs `BIGSERIAL`
repartant de `1`. Donc `entrepot.id = 1` existe **au Brésil ET en Équateur** : les
`id` / `paysId` numériques **entrent en collision** une fois consolidés. La dimension
pays (`codePays`) doit donc être **explicite** dans chaque réponse. La clé d'unicité
côté frontend est le couple **`{ codePays, id }`**. Les `id` locaux ne sont **pas** réécrits.

---

## 2. Découverte des pays

1. Le central tient un registre des backends locaux (`url` + `codePays` attendu), via config.
2. Au démarrage / périodiquement, appeler `GET {baseLocal}/api/v1/pays` sur chaque local pour
   récupérer `codePays` et `nomPays` (l'objet `Pays` complet sert aussi aux seuils des jauges).
3. Pour les endpoints de liste, faire un **fan-out** vers tous les locaux *up*, puis envelopper
   chaque réponse dans son groupe pays.

---

## 3. Règles de sérialisation imposées

Ces règles garantissent que `frontend-web/src/types/api.ts` reste valide.

| Type Java         | JSON attendu            | Note                                                     |
| ----------------- | ----------------------- | -------------------------------------------------------- |
| `Long`, `Integer` | `number`                | —                                                        |
| `BigDecimal`      | `number`                | **Ne pas** activer `WRITE_BIGDECIMAL_AS_PLAIN` en string |
| `String`          | `string`                | —                                                        |
| `Boolean`         | `boolean`               | —                                                        |
| `LocalDate`       | `"2026-06-01"`          | ISO-8601, `jackson-datatype-jsr310`                      |
| `LocalDateTime`   | `"2026-06-01T14:32:00"` | `WRITE_DATES_AS_TIMESTAMPS=false`                        |
| `enum`            | `"OUVERTE"`             | **UPPERCASE** (`.name()` Jackson par défaut)             |

- Tous les noms de champs sont en **camelCase**.
- **Aucune** transformation des enums en minuscules : on garde `OUVERTE`, `CONFORME`,
  `CRITIQUE`, etc. (le frontend compte dessus).

---

## 4. Contrat de sortie endpoint par endpoint

Toutes les routes sont préfixées par `/api/v1`. Les `{codePays}` valent `BR` / `EC` / `CO`.

### 4.1 `GET /pays` → `Pays[]`

Une ligne par backend local (sa propre config seuils/tolérances).

```json
[
  {
    "id": 1,
    "codePays": "BR",
    "nomPays": "Brésil",
    "temperatureIdealeC": 29.0,
    "humiditeIdealePourcent": 55.0,
    "toleranceTemperatureC": 3.0,
    "toleranceHumiditePourcent": 2.0,
    "estActif": true
  }
]
```

### 4.2 `GET /exploitations` → `CountryGroup<Exploitation>[]`

```json
[
  {
    "codePays": "BR",
    "nomPays": "Brésil",
    "data": [
      {
        "id": 1,
        "nomExploitation": "Fazenda Santa Rita",
        "localisation": "Minas Gerais",
        "responsableEmail": "responsable.br@futurekawa.local",
        "estActive": true,
        "paysId": 1,
        "codePays": "BR"
      }
    ]
  }
]
```

### 4.3 `GET /entrepots` (`?exploitationId`) → `CountryGroup<Entrepot>[]`

```json
[
  {
    "codePays": "BR",
    "nomPays": "Brésil",
    "data": [
      {
        "id": 1,
        "nomEntrepot": "Entrepôt A",
        "localisation": "Hangar Nord",
        "capaciteMax": 5000,
        "statutEntrepot": "actif",
        "exploitationId": 1,
        "paysId": 1
      }
    ]
  }
]
```

### 4.4 `GET /lots` (`?statutLot`, `&page`, `&size`) → `CountryPageGroup<Lot>[]`

Pagination Spring `Page<T>` **à l'intérieur de chaque groupe pays**. Tri FIFO `dateEntreeStockage ASC`.

```json
[
  {
    "codePays": "BR",
    "nomPays": "Brésil",
    "page": {
      "content": [
        {
          "id": 1,
          "referenceLot": "BR-2026-0001",
          "dateEntreeStockage": "2026-01-15",
          "dateRecolte": "2025-12-20",
          "statutLot": "CONFORME",
          "qualiteLot": "Arabica AA",
          "exploitationId": 1,
          "entrepotId": 1,
          "paysId": 1,
          "ancienneteJours": 153
        }
      ],
      "totalElements": 42,
      "totalPages": 3,
      "number": 0,
      "size": 20,
      "numberOfElements": 20,
      "first": true,
      "last": false,
      "empty": false
    }
  }
]
```

### 4.5 `GET /alertes` (`?statutAlerte`, `&typeAlerte`, `&page`, `&size`) → `CountryPageGroup<Alerte>[]`

Même enveloppe que les lots. Tri `dateHeureCreation DESC`.

```json
[
  {
    "codePays": "BR",
    "nomPays": "Brésil",
    "page": {
      "content": [
        {
          "id": 1,
          "typeAlerte": "CONDITION_NON_IDEALE",
          "niveau": "CRITIQUE",
          "statutAlerte": "OUVERTE",
          "messageDescription": "Température 35.0°C (idéale 29.0 ± 3.0)",
          "dateHeureCreation": "2026-06-17T08:32:00",
          "dateHeureCloture": null,
          "entrepotId": 1,
          "lotId": null,
          "paysId": 1
        }
      ],
      "totalElements": 5,
      "totalPages": 1,
      "number": 0,
      "size": 20,
      "numberOfElements": 5,
      "first": true,
      "last": true,
      "empty": false
    }
  }
]
```

### 4.6 `GET /entrepots/{codePays}/{id}/mesures` (`?from`, `&to`, `&page`, `&size`) → `Page<MesureStockage>`

Une mesure cible un entrepôt unique → **un seul pays** → `Page` simple, **pas** de groupement.

```json
{
  "content": [
    {
      "id": 1024,
      "idCapteur": "esp32-br-01",
      "dateHeureMesure": "2026-06-17T08:30:00",
      "temperatureC": 35.0,
      "humiditePourcent": 58.0,
      "entrepotId": 1,
      "lotId": null
    }
  ],
  "totalElements": 1440,
  "totalPages": 72,
  "number": 0,
  "size": 20,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

### 4.7 Ressources unitaires

- `GET /entrepots/{codePays}/{id}/mesures/latest` → `MesureStockage`
- `GET /lots/{codePays}/{id}` → `Lot`
- `GET /alertes/{codePays}/{id}` → `Alerte`
- `GET /entrepots/{codePays}/{id}` → `Entrepot`

### 4.8 Écritures (relayées au bon backend local selon `{codePays}`)

| Méthode | Route centrale             | Corps                                                  | Réponse     | Relais local                        |
| ------- | -------------------------- | ------------------------------------------------------ | ----------- | ----------------------------------- |
| `POST`  | `/lots`                    | `CreateLotRequest` (inclut `codePays` pour le routage) | `Lot` (201) | `POST {local}/api/v1/lots`          |
| `PATCH` | `/lots/{codePays}/{id}`    | `UpdateLotRequest`                                     | `Lot`       | `PATCH {local}/api/v1/lots/{id}`    |
| `PATCH` | `/alertes/{codePays}/{id}` | `UpdateAlerteRequest`                                  | `Alerte`    | `PATCH {local}/api/v1/alertes/{id}` |

> `POST /lots` : ajouter `codePays` au corps côté frontend (ou le déduire d'un en-tête) pour
> que le central sache vers quel backend local router la création.

---

## 5. Tolérance aux pannes (circuit breaker)

- Si un backend local est **indisponible** (circuit ouvert / timeout), son groupe pays est
  **omis** des tableaux `CountryGroup` / `CountryPageGroup`.
- Le central renvoie tout de même `200 OK` avec les pays disponibles, et signale les pays
  manquants via un en-tête de réponse :

```
X-Unavailable-Countries: EC,CO
```

- Le frontend reste fonctionnel pour les pays *up* et peut afficher un bandeau d'avertissement
  pour les pays absents.
- Pour une ressource unitaire (`/lots/{codePays}/{id}`) dont le pays est down : renvoyer
  `503 Service Unavailable` avec un corps d'erreur explicite.

---

## 6. Correspondance champ local → consolidé

| Ressource        | Champs conservés tels quels                | Ajout par le central                             |
| ---------------- | ------------------------------------------ | ------------------------------------------------ |
| `Pays`           | tous                                       | —                                                |
| `Exploitation`   | tous (`codePays` déjà fourni par le local) | —                                                |
| `Entrepot`       | tous                                       | enveloppe `CountryGroup` (`codePays`, `nomPays`) |
| `Lot`            | tous                                       | enveloppe `CountryPageGroup`                     |
| `Alerte`         | tous                                       | enveloppe `CountryPageGroup`                     |
| `MesureStockage` | tous                                       | aucune (un seul pays par requête)                |

Rappel : `id` et `paysId` restent **locaux** ; l'unicité globale passe par `codePays`.

---

## 7. À faire à l'arrivée du vrai backend central

1. Générer son `openapi.yml` et le confronter à ce contrat + à `frontend-web/src/types/api.ts`.
2. Ajuster les deux miroirs si l'enveloppe réelle diffère (un seul point de vérité à la fois).
