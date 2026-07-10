import type { NextDrawResponse } from "@/types/dashboard";

/** Mirrors `dashboard.interfaces.rest.dto.OrganizerDashboardResponse` exactly. */
export interface OrganizerDashboardResponse {
  groups: OrganizerGroupResponse[];
  quickActions: QuickActionResponse[];
}

export interface OrganizerGroupResponse {
  groupId: string;
  groupCode: string;
  name: string;
  memberCount: number;
  maximumMembers: number;
  hasActiveInvitation: boolean;
  nextDraw: NextDrawResponse | null;
  contributionProgressPercent: number;
}

export interface QuickActionResponse {
  label: string;
  method: string;
  path: string;
}
