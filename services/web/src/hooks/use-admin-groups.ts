"use client";

import { useQuery } from "@tanstack/react-query";

import { listPlatformGroups } from "@/services/admin-service";
import type { AdminGroupSearchParams } from "@/types/admin";

export function useAdminGroups(params: AdminGroupSearchParams) {
  return useQuery({
    queryKey: ["admin", "groups", params] as const,
    queryFn: () => listPlatformGroups(params),
  });
}
