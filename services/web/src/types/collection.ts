/** Mirrors `payment.interfaces.rest.dto.MemberCollectionResponse` exactly. */
export interface MemberCollectionResponse {
  memberId: string;
  memberName: string | null;
  status: string;
  expectedAmountPaise: number;
  collectedAmountPaise: number;
  paidAt: string | null;
  dueDate: string;
}

/** Mirrors `payment.interfaces.rest.dto.CollectionSummaryResponse` exactly. */
export interface CollectionSummaryResponse {
  groupId: string;
  cycleActive: boolean;
  cycleNumber: number;
  cycleStart: string | null;
  cycleEnd: string | null;
  dueDate: string | null;
  contributionAmountPaise: number;
  currencyCode: string;
  totalMembers: number;
  paidCount: number;
  pendingCount: number;
  overdueCount: number;
  totalExpectedPaise: number;
  totalCollectedPaise: number;
  totalRemainingPaise: number;
  members: MemberCollectionResponse[];
}
