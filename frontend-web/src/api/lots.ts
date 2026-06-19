import { http } from '@/lib/http';
import type { LotsResponse, Lot, StatutLot, CreateLotRequest, UpdateLotRequest } from '@/types/api';

export interface ListLotsParams {
  statutLot?: StatutLot;
  page?: number;
  size?: number;
}

export async function listLots(params: ListLotsParams = {}): Promise<LotsResponse> {
  const { data } = await http.get<LotsResponse>('/lots', { params });
  return data;
}

export async function createLot(body: CreateLotRequest & { codePays: string }): Promise<Lot> {
  const { data } = await http.post<Lot>('/lots', body);
  return data;
}

export async function updateLotStatut(
  codePays: string,
  id: number,
  body: UpdateLotRequest,
): Promise<Lot> {
  const { data } = await http.patch<Lot>(`/lots/${codePays}/${id}`, body);
  return data;
}
