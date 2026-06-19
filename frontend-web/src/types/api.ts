// ============================================================
// FutureKawa — Types de l'API du backend central (consolidé)
// Source de vérité : DTO des backends locaux (Spring Boot)
// Réponses groupées par pays + pagination Spring Page<T>
// ============================================================

// ---------- Enums (UPPERCASE, .name() Jackson) ----------

export type StatutLot = 'CONFORME' | 'EN_ALERTE' | 'PERIME';
export type StatutAlerte = 'OUVERTE' | 'NOTIFIEE' | 'CLOTUREE';
export type NiveauAlerte = 'INFO' | 'WARNING' | 'CRITIQUE';
export type TypeAlerte = 'CONDITION_NON_IDEALE' | 'LOT_TROP_ANCIEN';

// ---------- Enveloppes génériques ----------

/** Miroir de l'objet Page<T> de Spring Data. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // index de page (0-based)
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort?: { sorted: boolean; unsorted: boolean; empty: boolean };
}

/** Groupe une liste simple (non paginée) par pays d'origine. */
export interface CountryGroup<T> {
  codePays: string; // "BR", "EC", "CO"
  nomPays: string; // "Brésil", "Équateur", "Colombie"
  data: T[];
}

/** Groupe une page (paginée) par pays d'origine. */
export interface CountryPageGroup<T> {
  codePays: string;
  nomPays: string;
  page: Page<T>;
}

// ---------- Ressources (miroir des *Response locaux) ----------

export interface Pays {
  id: number;
  codePays: string;
  nomPays: string;
  temperatureIdealeC: number;
  humiditeIdealePourcent: number;
  toleranceTemperatureC: number;
  toleranceHumiditePourcent: number;
  estActif: boolean;
}

export interface Exploitation {
  id: number;
  nomExploitation: string;
  localisation: string | null;
  responsableEmail: string | null;
  estActive: boolean;
  paysId: number;
  codePays: string;
}

export interface Entrepot {
  id: number;
  nomEntrepot: string;
  localisation: string | null;
  capaciteMax: number | null;
  statutEntrepot: string; // ex. "actif"
  exploitationId: number;
  paysId: number;
}

export interface Lot {
  id: number;
  referenceLot: string;
  dateEntreeStockage: string; // ISO date
  dateRecolte: string | null; // ISO date
  statutLot: StatutLot;
  qualiteLot: string | null;
  exploitationId: number;
  entrepotId: number;
  paysId: number;
  ancienneteJours: number; // calculé côté backend
}

export interface MesureStockage {
  id: number;
  idCapteur: string;
  dateHeureMesure: string; // ISO date-time
  temperatureC: number;
  humiditePourcent: number;
  entrepotId: number;
  lotId: number | null;
}

export interface Alerte {
  id: number;
  typeAlerte: TypeAlerte;
  niveau: NiveauAlerte;
  statutAlerte: StatutAlerte;
  messageDescription: string;
  dateHeureCreation: string; // ISO date-time
  dateHeureCloture: string | null; // null si ouverte
  entrepotId: number;
  lotId: number | null;
  paysId: number;
}

// ---------- Types de réponse par endpoint (consolidé) ----------

export type PaysListResponse = Pays[]; // GET /api/v1/pays
export type ExploitationsResponse = CountryGroup<Exploitation>[]; // GET /api/v1/exploitations
export type EntrepotsResponse = CountryGroup<Entrepot>[]; // GET /api/v1/entrepots
export type LotsResponse = CountryPageGroup<Lot>[]; // GET /api/v1/lots
export type AlertesResponse = CountryPageGroup<Alerte>[]; // GET /api/v1/alertes
export type MesuresResponse = Page<MesureStockage>; // GET /entrepots/{codePays}/{id}/mesures
// Unitaires : Lot | Alerte | Entrepot | MesureStockage (GET .../{id}, .../latest)

// ---------- Corps de requête (écritures relayées au local) ----------

export interface CreateLotRequest {
  referenceLot: string;
  dateEntreeStockage: string; // ISO date
  dateRecolte?: string | null; // ISO date
  qualiteLot?: string | null;
  exploitationId: number;
  entrepotId: number;
}

export interface UpdateLotRequest {
  statutLot: StatutLot;
}

export interface UpdateAlerteRequest {
  statutAlerte: StatutAlerte;
}
