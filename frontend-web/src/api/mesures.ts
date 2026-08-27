import { http } from '@/lib/http';
import { estIndisponible, estIntrouvable } from '@/lib/errors';
import type { MesuresResponse, MesureStockage } from '@/types/api';

export interface ListMesuresParams {
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export async function listMesures(
  codePays: string,
  entrepotId: number,
  params: ListMesuresParams = {},
): Promise<MesuresResponse> {
  const { data } = await http.get<MesuresResponse>(`/entrepots/${codePays}/${entrepotId}/mesures`, {
    params,
  });
  return data;
}

export async function latestMesure(codePays: string, entrepotId: number): Promise<MesureStockage> {
  const { data } = await http.get<MesureStockage>(
    `/entrepots/${codePays}/${entrepotId}/mesures/latest`,
  );
  return data;
}

/**
 * Dernière mesure, ou `null` quand il n'y en a aucune (404) ou que le pays est
 * momentanément indisponible (503).
 *
 * Un entrepôt fraîchement créé n'a pas encore de mesure : c'est un état normal,
 * pas une panne. Les vues qui affichent une liste d'entrepôts doivent donc pouvoir
 * continuer sans celle-ci.
 */
export async function latestMesureOrNull(
  codePays: string,
  entrepotId: number,
): Promise<MesureStockage | null> {
  try {
    return await latestMesure(codePays, entrepotId);
  } catch (error) {
    if (estIntrouvable(error) || estIndisponible(error)) return null;
    throw error;
  }
}
