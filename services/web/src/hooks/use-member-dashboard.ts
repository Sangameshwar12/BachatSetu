"use client";

import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/services/api-client";
import { getMemberDashboard } from "@/services/dashboard-service";

export const memberDashboardQueryKey = ["dashboard", "member"] as const;

export function useMemberDashboard() {
  return useQuery({
    queryKey: memberDashboardQueryKey,
    queryFn: getMemberDashboard,
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.code === "no-active-group") {
        return false;
      }
      return failureCount < 1;
    },
  });
}

/** True when the dashboard query failed specifically because the member has no active group yet. */
export function isNoActiveGroupError(error: unknown): boolean {
  return error instanceof ApiError && error.code === "no-active-group";
}
