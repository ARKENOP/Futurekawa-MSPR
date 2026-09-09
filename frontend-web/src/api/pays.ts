import { http } from '@/lib/http';
import type { PaysListResponse } from '@/types/api';

export async function listPays(): Promise<PaysListResponse> {
  const { data } = await http.get<PaysListResponse>('/pays');
  return data;
}
