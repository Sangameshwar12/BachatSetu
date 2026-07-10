"use client";

import { useQuery } from "@tanstack/react-query";

import { getPlatformOverview } from "@/services/platform-operations-service";

export function usePlatformOverview() {
  return useQuery({ queryKey: ["platform-operations", "overview"] as const, queryFn: getPlatformOverview });
}
