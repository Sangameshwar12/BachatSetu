import { apiClient } from "@/services/api-client";
import type { CloseDrawRequest, DrawResponse } from "@/types/draw";

/** `GET /api/v1/draws/{drawId}` — one tenant-scoped draw, including auction bids. */
export async function getDraw(drawId: string): Promise<DrawResponse> {
  const { data } = await apiClient.get<DrawResponse>(`/api/v1/draws/${drawId}`);
  return data;
}

/** `PATCH /api/v1/draws/{drawId}/conduct` — opens a scheduled draw. Owner-only. */
export async function conductDraw(drawId: string): Promise<DrawResponse> {
  const { data } = await apiClient.patch<DrawResponse>(`/api/v1/draws/${drawId}/conduct`);
  return data;
}

/** `PATCH /api/v1/draws/{drawId}/close` — closes an open draw with its winning member. Owner-only. */
export async function closeDraw(drawId: string, payload: CloseDrawRequest): Promise<DrawResponse> {
  const { data } = await apiClient.patch<DrawResponse>(`/api/v1/draws/${drawId}/close`, payload);
  return data;
}
