import { http } from '@/lib/http';
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
