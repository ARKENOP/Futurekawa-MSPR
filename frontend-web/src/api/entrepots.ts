import { http } from '@/lib/http';
import type { EntrepotsResponse, Entrepot } from '@/types/api';

export async function listEntrepots(exploitationId?: number): Promise<EntrepotsResponse> {
  const { data } = await http.get<EntrepotsResponse>('/entrepots', {
    params: exploitationId ? { exploitationId } : undefined,
  });
  return data;
}

export async function getEntrepot(codePays: string, id: number): Promise<Entrepot> {
  const { data } = await http.get<Entrepot>(`/entrepots/${codePays}/${id}`);
  return data;
}
