export type GroupType = "BHISHI" | "SELF_HELP_GROUP" | "SOCIETY_COLLECTION" | "COMMUNITY_FUND";

export type ContributionFrequency = "WEEKLY" | "MONTHLY" | "QUARTERLY";

export type PayoutMethod = "FIXED_ROTATION" | "RANDOM_DRAW" | "AUCTION";

/** Mirrors `group.interfaces.rest.dto.ContributionScheduleRequest` exactly. */
export interface ContributionScheduleRequest {
  contributionAmountPaise: number;
  frequency: ContributionFrequency;
  /** ISO date (`yyyy-MM-dd`) — the backend binds this to a `java.time.LocalDate`. */
  startDate: string;
  cycleCount: number;
}

/** Mirrors `group.interfaces.rest.dto.MemberCapacityRequest` exactly. */
export interface MemberCapacityRequest {
  minimum: number;
  maximum: number;
}

/** Mirrors `group.interfaces.rest.dto.GroupRuleRequest` exactly. */
export interface GroupRuleRequest {
  contributionSchedule: ContributionScheduleRequest;
  memberCapacity: MemberCapacityRequest;
  payoutMethod: PayoutMethod;
  partialPaymentsAllowed: boolean;
}

/** Mirrors `group.interfaces.rest.dto.CreateSavingsGroupRequest` exactly. */
export interface CreateSavingsGroupRequest {
  name: string;
  description?: string;
  type: GroupType;
  rule: GroupRuleRequest;
}

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
