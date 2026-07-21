import { apiClient } from "@/services/api-client";
import type { CreateSavingsGroupRequest, SavingsGroupResponse } from "@/types/group";

/** `POST /api/v1/groups` — creates a new inactive savings group owned by the caller. */
export async function createGroup(payload: CreateSavingsGroupRequest): Promise<SavingsGroupResponse> {
  const { data } = await apiClient.post<SavingsGroupResponse>("/api/v1/groups", payload);
  return data;
}

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

/** `DELETE /api/v1/groups/{groupId}/members/{memberId}` — organizer-only. Removes a non-owner member. */
export async function removeMember(groupId: string, memberId: string): Promise<void> {
  await apiClient.delete(`/api/v1/groups/${groupId}/members/${memberId}`);
}
