import { apiClient } from "@/services/api-client";
import type { SavingsGroupResponse } from "@/types/group";

/** `GET /api/v1/groups/{groupId}` — one tenant-scoped savings group. */
export async function getGroup(groupId: string): Promise<SavingsGroupResponse> {
  const { data } = await apiClient.get<SavingsGroupResponse>(`/api/v1/groups/${groupId}`);
  return data;
}

/** `PATCH /api/v1/groups/{groupId}/activate` — organizer-only. Activates an inactive/suspended group. */
export async function activateGroup(groupId: string): Promise<SavingsGroupResponse> {
  const { data } = await apiClient.patch<SavingsGroupResponse>(`/api/v1/groups/${groupId}/activate`);
  return data;
}

/** `PATCH /api/v1/groups/{groupId}/suspend` — organizer-only. Suspends an active group. */
export async function suspendGroup(groupId: string): Promise<SavingsGroupResponse> {
  const { data } = await apiClient.patch<SavingsGroupResponse>(`/api/v1/groups/${groupId}/suspend`);
  return data;
}

/** `PATCH /api/v1/groups/{groupId}/close` — organizer-only. Permanently closes the group. */
export async function closeGroup(groupId: string): Promise<SavingsGroupResponse> {
  const { data } = await apiClient.patch<SavingsGroupResponse>(`/api/v1/groups/${groupId}/close`);
  return data;
}
