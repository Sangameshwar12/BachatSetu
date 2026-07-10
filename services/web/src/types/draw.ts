export interface AuctionBidResponse {
  bidId: string;
  memberId: string;
  amountPaise: number;
  submittedAt: string;
}

/** Mirrors `draw.interfaces.rest.dto.DrawResponse` exactly. */
export interface DrawResponse {
  drawId: string;
  tenantId: string;
  groupId: string;
  cycleId: string;
  drawNumber: number;
  type: string;
  status: string;
  scheduledAt: string;
  winnerMemberId: string | null;
  bids: AuctionBidResponse[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

/** Mirrors `draw.interfaces.rest.dto.DrawSummaryResponse` exactly. */
export interface DrawSummaryResponse {
  drawId: string;
  drawNumber: number;
  type: string;
  status: string;
  scheduledAt: string;
  winnerMemberId: string | null;
}

/** Mirrors `draw.interfaces.rest.dto.CloseDrawRequest` exactly. */
export interface CloseDrawRequest {
  winnerId: string;
}
