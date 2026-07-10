"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { activateTenant, archiveTenant, searchTenants, suspendTenant } from "@/services/platform-operations-service";
import type { SuspendTenantRequest } from "@/types/platform-operations";

export function usePlatformTenants(status: string | undefined, page: number, size: number) {
  return useQuery({
    queryKey: ["platform-operations", "tenants", { status, page, size }] as const,
    queryFn: () => searchTenants(status, page, size),
  });
}

function useInvalidateTenants() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["platform-operations", "tenants"] });
}

export function useSuspendTenant() {
  const invalidate = useInvalidateTenants();
  return useMutation({
    mutationFn: ({ tenantId, request }: { tenantId: string; request: SuspendTenantRequest }) =>
      suspendTenant(tenantId, request),
    onSuccess: invalidate,
  });
}

export function useActivateTenant() {
  const invalidate = useInvalidateTenants();
  return useMutation({
    mutationFn: (tenantId: string) => activateTenant(tenantId),
    onSuccess: invalidate,
  });
}

export function useArchiveTenant() {
  const invalidate = useInvalidateTenants();
  return useMutation({
    mutationFn: (tenantId: string) => archiveTenant(tenantId),
    onSuccess: invalidate,
  });
}
