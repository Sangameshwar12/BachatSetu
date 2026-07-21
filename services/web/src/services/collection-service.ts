import { apiClient } from "@/services/api-client";
import type { CollectionSummaryResponse } from "@/types/collection";

/** `GET /api/v1/groups/{groupId}/collection` — the group's current-cycle contribution status. */
export async function getCollectionSummary(groupId: string): Promise<CollectionSummaryResponse> {
  const { data } = await apiClient.get<CollectionSummaryResponse>(`/api/v1/groups/${groupId}/collection`);
  return data;
}

/** `POST /api/v1/groups/{groupId}/collection/members/{memberId}/mark-paid` — organizer-only. */
export async function markMemberPaid(groupId: string, memberId: string): Promise<void> {
  await apiClient.post(`/api/v1/groups/${groupId}/collection/members/${memberId}/mark-paid`);
}
