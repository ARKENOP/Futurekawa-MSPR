export type StatutLot = 'CONFORME' | 'EN_ALERTE' | 'PERIME';
export type StatutAlerte = 'OUVERTE' | 'NOTIFIEE' | 'CLOTUREE';
export type NiveauAlerte = 'INFO' | 'WARNING' | 'CRITIQUE';
export type TypeAlerte = 'CONDITION_NON_IDEALE' | 'LOT_TROP_ANCIEN';

/** Miroir de l'objet Page<T> de Spring Data. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort?: { sorted: boolean; unsorted: boolean; empty: boolean };
}

/** Groupe une liste simple (non paginée) par pays d'origine. */
export interface CountryGroup<T> {
  codePays: string;
  nomPays: string;
  data: T[];
}

/** Groupe une page (paginée) par pays d'origine. */
export interface CountryPageGroup<T> {
  codePays: string;
  nomPays: string;
  page: Page<T>;
}

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
  statutEntrepot: string;
  exploitationId: number;
  paysId: number;
}

export interface Lot {
  id: number;
  referenceLot: string;
  dateEntreeStockage: string;
  dateRecolte: string | null;
  statutLot: StatutLot;
  qualiteLot: string | null;
  exploitationId: number;
  entrepotId: number;
  paysId: number;
  ancienneteJours: number;
}

export interface MesureStockage {
  id: number;
  idCapteur: string;
  dateHeureMesure: string;
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
  dateHeureCreation: string;
  dateHeureCloture: string | null;
  entrepotId: number;
  lotId: number | null;
  paysId: number;
}

export type PaysListResponse = Pays[];
export type ExploitationsResponse = CountryGroup<Exploitation>[];
export type EntrepotsResponse = CountryGroup<Entrepot>[];
export type LotsResponse = CountryPageGroup<Lot>[];
export type AlertesResponse = CountryPageGroup<Alerte>[];
export type MesuresResponse = Page<MesureStockage>;

export interface CreateLotRequest {
  referenceLot: string;
  dateEntreeStockage: string;
  dateRecolte?: string | null;
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
