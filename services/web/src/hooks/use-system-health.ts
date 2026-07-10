"use client";

import { useQuery } from "@tanstack/react-query";

import { getSystemHealth } from "@/services/platform-operations-service";

export function useSystemHealth() {
  return useQuery({ queryKey: ["platform-operations", "health"] as const, queryFn: getSystemHealth });
}
