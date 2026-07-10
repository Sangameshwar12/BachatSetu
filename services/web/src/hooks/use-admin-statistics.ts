"use client";

import { useQuery } from "@tanstack/react-query";

import { getPlatformStatistics } from "@/services/admin-service";

export function useAdminStatistics() {
  return useQuery({
    queryKey: ["admin", "statistics"] as const,
    queryFn: getPlatformStatistics,
  });
}
