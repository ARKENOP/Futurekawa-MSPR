import { http } from '@/lib/http';
import type {
  AlertesResponse,
  Alerte,
  StatutAlerte,
  TypeAlerte,
  UpdateAlerteRequest,
} from '@/types/api';

export interface ListAlertesParams {
  statutAlerte?: StatutAlerte;
  typeAlerte?: TypeAlerte;
  page?: number;
  size?: number;
}

export async function listAlertes(params: ListAlertesParams = {}): Promise<AlertesResponse> {
  const { data } = await http.get<AlertesResponse>('/alertes', { params });
  return data;
}

export async function updateAlerteStatut(
  codePays: string,
  id: number,
  body: UpdateAlerteRequest,
): Promise<Alerte> {
  const { data } = await http.patch<Alerte>(`/alertes/${codePays}/${id}`, body);
  return data;
}

export function closeAlerte(codePays: string, id: number): Promise<Alerte> {
  return updateAlerteStatut(codePays, id, { statutAlerte: 'CLOTUREE' });
}
