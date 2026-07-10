/** Mirrors `group.interfaces.rest.dto.SavingsGroupResponse` exactly. */
export interface SavingsGroupResponse {
  groupId: string;
  tenantId: string;
  ownerId: string;
  groupCode: string;
  name: string;
  description: string | null;
  type: string;
  status: string;
  contributionAmountPaise: number;
  currencyCode: string;
  maximumMembers: number;
  activeMemberCount: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

/** Mirrors `group.interfaces.rest.dto.SavingsGroupSummaryResponse` exactly. */
export interface SavingsGroupSummaryResponse {
  groupId: string;
  groupCode: string;
  name: string;
  status: string;
  contributionAmountPaise: number;
  currencyCode: string;
  maximumMembers: number;
  activeMemberCount: number;
}
