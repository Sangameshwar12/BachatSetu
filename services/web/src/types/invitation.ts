/** Mirrors `invitation.interfaces.rest.dto.InvitationPreviewResponse` exactly. */
export interface InvitationPreviewResponse {
  groupName: string;
  organizerName: string;
  contributionAmountPaise: number;
  currencyCode: string;
  frequency: string;
  memberCount: number;
  maximumMembers: number;
  status: string;
}

export type JoinChannel = "CODE" | "QR" | "LINK";

/** Mirrors `invitation.interfaces.rest.dto.CreateInvitationRequest` exactly. */
export interface CreateInvitationRequest {
  type: JoinChannel;
}

/** Mirrors `invitation.interfaces.rest.dto.InvitationResponse` exactly. */
export interface InvitationResponse {
  invitationId: string;
  groupId: string;
  code: string;
  joinLink: string;
  type: JoinChannel;
  status: string;
  expiresAt: string;
}

/** Mirrors `invitation.interfaces.rest.dto.AcceptInvitationRequest`. Exactly one of code/token is set. */
export interface AcceptInvitationRequest {
  code?: string;
  token?: string;
  channel: JoinChannel;
}

/** Mirrors `invitation.interfaces.rest.dto.AcceptInvitationResponse` exactly. */
export interface AcceptInvitationResponse {
  groupId: string;
  memberId: string;
  joinedAt: string;
}
