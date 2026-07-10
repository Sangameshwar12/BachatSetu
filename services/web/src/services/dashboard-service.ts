import { apiClient } from "@/services/api-client";
import type { MemberDashboardResponse } from "@/types/dashboard";

/**
 * `GET /api/v1/dashboard/member` — composed member home screen. Returns 404 (surfaced as
 * `ApiError` with `code: "no-active-group"`-shaped problem detail) when the caller has no
 * active group yet; callers should treat that as "show the Welcome / Join a Group state."
 */
export async function getMemberDashboard(): Promise<MemberDashboardResponse> {
  const { data } = await apiClient.get<MemberDashboardResponse>("/api/v1/dashboard/member");
  return data;
}
