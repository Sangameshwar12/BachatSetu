/**
 * Mirrors `dashboard.interfaces.rest.dto.MemberDashboardResponse` exactly.
 *
 * `GET /api/v1/dashboard/member` returns 404 (not a 200 with a null group) when the caller has
 * no active group — so `currentGroup` is always present on a successful response. `nextDraw` and
 * `latestPaymentStatus` are independently optional; `recentNotifications` is always a list,
 * possibly empty.
 */
export interface MemberDashboardResponse {
  currentGroup: CurrentGroupResponse;
  nextDraw: NextDrawResponse | null;
  latestPaymentStatus: string | null;
  recentNotifications: DashboardNotificationSummary[];
}

export interface CurrentGroupResponse {
  groupId: string;
  groupCode: string;
  name: string;
  upcomingInstallmentAmountPaise: number;
  currencyCode: string;
  frequency: string;
  memberCount: number;
  maximumMembers: number;
}

export interface NextDrawResponse {
  drawId: string;
  scheduledAt: string;
  status: string;
}

export interface DashboardNotificationSummary {
  notificationId: string;
  category: string;
  status: string;
  createdAt: string;
}
