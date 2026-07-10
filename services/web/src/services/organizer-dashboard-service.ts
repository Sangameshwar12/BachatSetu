import { apiClient } from "@/services/api-client";
import type { OrganizerDashboardResponse } from "@/types/organizer-dashboard";

/**
 * `GET /api/v1/dashboard/organizer` — every group the caller owns, with member counts,
 * pending-invitation status, next scheduled draw, and contribution progress per group. Returns
 * an empty `groups` array (200, not 404) if the caller owns no groups.
 */
export async function getOrganizerDashboard(): Promise<OrganizerDashboardResponse> {
  const { data } = await apiClient.get<OrganizerDashboardResponse>("/api/v1/dashboard/organizer");
  return data;
}
