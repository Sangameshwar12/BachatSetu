import { apiClient } from "@/services/api-client";
import type {
  AcceptInvitationRequest,
  AcceptInvitationResponse,
  CreateInvitationRequest,
  InvitationPreviewResponse,
  InvitationResponse,
} from "@/types/invitation";

/** `GET /api/v1/join/{token}` — public, unauthenticated preview of a group by invitation token. */
export async function previewInvitation(token: string): Promise<InvitationPreviewResponse> {
  const { data } = await apiClient.get<InvitationPreviewResponse>(`/api/v1/join/${token}`);
  return data;
}

/** `POST /api/v1/groups/join` — authenticated join by code or token. */
export async function joinGroup(payload: AcceptInvitationRequest): Promise<AcceptInvitationResponse> {
  const { data } = await apiClient.post<AcceptInvitationResponse>("/api/v1/groups/join", payload);
  return data;
}

/**
 * `POST /api/v1/groups/{groupId}/invite` — organizer-only. Generates a new invitation
 * (code + token), revoking any prior active one for the group.
 */
export async function createInvitation(
  groupId: string,
  payload: CreateInvitationRequest
): Promise<InvitationResponse> {
  const { data } = await apiClient.post<InvitationResponse>(
    `/api/v1/groups/${groupId}/invite`,
    payload
  );
  return data;
}

/** `GET /api/v1/groups/{groupId}/invite` — organizer-only. The group's current active invitation. */
export async function getCurrentInvitation(groupId: string): Promise<InvitationResponse> {
  const { data } = await apiClient.get<InvitationResponse>(`/api/v1/groups/${groupId}/invite`);
  return data;
}

/** `DELETE /api/v1/groups/{groupId}/invite` — organizer-only. Revokes the current invitation. */
export async function revokeInvitation(groupId: string): Promise<void> {
  await apiClient.delete(`/api/v1/groups/${groupId}/invite`);
}
