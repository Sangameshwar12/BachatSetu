"use client";

import { useQuery } from "@tanstack/react-query";

import { searchAudit } from "@/services/audit-service";
import type { AuditSearchParams } from "@/types/audit";

export function useAuditSearch(params: AuditSearchParams) {
  return useQuery({
    queryKey: ["audit", "search", params] as const,
    queryFn: () => searchAudit(params),
  });
}
