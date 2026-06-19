import type { Pays, Exploitation, Entrepot, Lot, Alerte, MesureStockage } from '@/types/api';

/** Métadonnées pays (codePays/nomPays) utilisées par les enveloppes groupées. */
export interface CountryMeta {
  codePays: string;
  nomPays: string;
}

export const countries: CountryMeta[] = [
  { codePays: 'BR', nomPays: 'Brésil' },
  { codePays: 'EC', nomPays: 'Équateur' },
  { codePays: 'CO', nomPays: 'Colombie' },
];

/** Un pays par base locale : id=1 dans chacune (collision volontaire, distinguée par codePays). */
export const pays: Record<string, Pays> = {
  BR: {
    id: 1,
    codePays: 'BR',
    nomPays: 'Brésil',
    temperatureIdealeC: 29.0,
    humiditeIdealePourcent: 55.0,
    toleranceTemperatureC: 3.0,
    toleranceHumiditePourcent: 2.0,
    estActif: true,
  },
  EC: {
    id: 1,
    codePays: 'EC',
    nomPays: 'Équateur',
    temperatureIdealeC: 24.0,
    humiditeIdealePourcent: 60.0,
    toleranceTemperatureC: 2.5,
    toleranceHumiditePourcent: 3.0,
    estActif: true,
  },
  CO: {
    id: 1,
    codePays: 'CO',
    nomPays: 'Colombie',
    temperatureIdealeC: 21.0,
    humiditeIdealePourcent: 58.0,
    toleranceTemperatureC: 2.0,
    toleranceHumiditePourcent: 2.5,
    estActif: true,
  },
};

export const exploitations: Record<string, Exploitation[]> = {
  BR: [
    {
      id: 1,
      nomExploitation: 'Fazenda Santa Rita',
      localisation: 'Minas Gerais',
      responsableEmail: 'responsable.br@futurekawa.local',
      estActive: true,
      paysId: 1,
      codePays: 'BR',
    },
  ],
  EC: [
    {
      id: 1,
      nomExploitation: 'Hacienda La Niebla',
      localisation: 'Loja',
      responsableEmail: 'responsable.ec@futurekawa.local',
      estActive: true,
      paysId: 1,
      codePays: 'EC',
    },
  ],
  CO: [
    {
      id: 1,
      nomExploitation: 'Finca El Mirador',
      localisation: 'Huila',
      responsableEmail: 'responsable.co@futurekawa.local',
      estActive: true,
      paysId: 1,
      codePays: 'CO',
    },
  ],
};

export const entrepots: Record<string, Entrepot[]> = {
  BR: [
    {
      id: 1,
      nomEntrepot: 'Entrepôt Norte',
      localisation: 'Hangar A',
      capaciteMax: 5000,
      statutEntrepot: 'actif',
      exploitationId: 1,
      paysId: 1,
    },
    {
      id: 2,
      nomEntrepot: 'Entrepôt Sul',
      localisation: 'Hangar B',
      capaciteMax: 3000,
      statutEntrepot: 'actif',
      exploitationId: 1,
      paysId: 1,
    },
  ],
  EC: [
    {
      id: 1,
      nomEntrepot: 'Bodega Central',
      localisation: 'Loja Ville',
      capaciteMax: 2500,
      statutEntrepot: 'actif',
      exploitationId: 1,
      paysId: 1,
    },
  ],
  CO: [
    {
      id: 1,
      nomEntrepot: 'Almacén Andes',
      localisation: 'Pitalito',
      capaciteMax: 4000,
      statutEntrepot: 'actif',
      exploitationId: 1,
      paysId: 1,
    },
  ],
};

export const lots: Record<string, Lot[]> = {
  BR: [
    {
      id: 1,
      referenceLot: 'BR-2026-0001',
      dateEntreeStockage: '2026-01-15',
      dateRecolte: '2025-12-20',
      statutLot: 'CONFORME',
      qualiteLot: 'Arabica AA',
      exploitationId: 1,
      entrepotId: 1,
      paysId: 1,
      ancienneteJours: 153,
    },
    {
      id: 2,
      referenceLot: 'BR-2025-0188',
      dateEntreeStockage: '2025-02-02',
      dateRecolte: '2025-01-10',
      statutLot: 'PERIME',
      qualiteLot: 'Robusta',
      exploitationId: 1,
      entrepotId: 2,
      paysId: 1,
      ancienneteJours: 500,
    },
    {
      id: 3,
      referenceLot: 'BR-2026-0042',
      dateEntreeStockage: '2026-03-10',
      dateRecolte: '2026-02-18',
      statutLot: 'EN_ALERTE',
      qualiteLot: 'Arabica A',
      exploitationId: 1,
      entrepotId: 1,
      paysId: 1,
      ancienneteJours: 99,
    },
  ],
  EC: [
    {
      id: 1,
      referenceLot: 'EC-2026-0007',
      dateEntreeStockage: '2026-04-01',
      dateRecolte: '2026-03-12',
      statutLot: 'CONFORME',
      qualiteLot: 'Arabica Altura',
      exploitationId: 1,
      entrepotId: 1,
      paysId: 1,
      ancienneteJours: 77,
    },
  ],
  CO: [
    {
      id: 1,
      referenceLot: 'CO-2026-0015',
      dateEntreeStockage: '2026-02-20',
      dateRecolte: '2026-01-30',
      statutLot: 'CONFORME',
      qualiteLot: 'Supremo',
      exploitationId: 1,
      entrepotId: 1,
      paysId: 1,
      ancienneteJours: 117,
    },
    {
      id: 2,
      referenceLot: 'CO-2026-0016',
      dateEntreeStockage: '2026-05-05',
      dateRecolte: '2026-04-15',
      statutLot: 'CONFORME',
      qualiteLot: 'Excelso',
      exploitationId: 1,
      entrepotId: 1,
      paysId: 1,
      ancienneteJours: 43,
    },
  ],
};

export const alertes: Record<string, Alerte[]> = {
  BR: [
    {
      id: 1,
      typeAlerte: 'CONDITION_NON_IDEALE',
      niveau: 'CRITIQUE',
      statutAlerte: 'OUVERTE',
      messageDescription: 'Température 35.0°C relevée (idéale 29.0 ± 3.0) — Entrepôt Norte',
      dateHeureCreation: '2026-06-17T08:32:00',
      dateHeureCloture: null,
      entrepotId: 1,
      lotId: null,
      paysId: 1,
    },
    {
      id: 2,
      typeAlerte: 'LOT_TROP_ANCIEN',
      niveau: 'WARNING',
      statutAlerte: 'OUVERTE',
      messageDescription: 'Lot BR-2025-0188 périmé (500 j > 365 j)',
      dateHeureCreation: '2026-06-16T01:00:00',
      dateHeureCloture: null,
      entrepotId: 2,
      lotId: 2,
      paysId: 1,
    },
    {
      id: 3,
      typeAlerte: 'CONDITION_NON_IDEALE',
      niveau: 'INFO',
      statutAlerte: 'CLOTUREE',
      messageDescription: 'Humidité revenue dans la plage idéale — Entrepôt Norte',
      dateHeureCreation: '2026-06-10T14:05:00',
      dateHeureCloture: '2026-06-10T18:20:00',
      entrepotId: 1,
      lotId: null,
      paysId: 1,
    },
  ],
  EC: [
    {
      id: 1,
      typeAlerte: 'CONDITION_NON_IDEALE',
      niveau: 'WARNING',
      statutAlerte: 'OUVERTE',
      messageDescription: 'Humidité 66.0% (idéale 60.0 ± 3.0) — Bodega Central',
      dateHeureCreation: '2026-06-17T06:10:00',
      dateHeureCloture: null,
      entrepotId: 1,
      lotId: null,
      paysId: 1,
    },
  ],
  CO: [],
};

/**
 * Génère une série temporelle de mesures pour un entrepôt.
 * BR/Entrepôt Norte (id 1) reçoit un pic hors-tolérance → déclenche le rouge cerise.
 */
export function generateMesures(
  codePays: string,
  entrepotId: number,
  count = 48,
): MesureStockage[] {
  const p = pays[codePays];
  const now = Date.now();
  const stepMs = 30 * 60 * 1000; // 30 min
  const spike = codePays === 'BR' && entrepotId === 1;

  const out: MesureStockage[] = [];
  for (let i = count - 1; i >= 0; i--) {
    const t = now - i * stepMs;
    const phase = (i / count) * Math.PI * 4;
    let temp = p.temperatureIdealeC + Math.sin(phase) * (p.toleranceTemperatureC * 0.6);
    const hum = p.humiditeIdealePourcent + Math.cos(phase) * (p.toleranceHumiditePourcent * 0.7);

    // Pic récent hors-tolérance pour la démo (dernières 3 mesures)
    if (spike && i < 3) {
      temp = p.temperatureIdealeC + p.toleranceTemperatureC * 2.4; // > 2× tolérance
    }

    out.push({
      id: entrepotId * 1000 + (count - i),
      idCapteur: `esp32-${codePays.toLowerCase()}-${entrepotId}`,
      dateHeureMesure: new Date(t).toISOString().slice(0, 19),
      temperatureC: Math.round(temp * 10) / 10,
      humiditePourcent: Math.round(hum * 10) / 10,
      entrepotId,
      lotId: null,
    });
  }
  return out;
}
