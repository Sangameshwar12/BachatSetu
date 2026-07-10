"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { disablePlatformUser, enablePlatformUser, listPlatformUsers } from "@/services/admin-service";
import type { AdminUserSearchParams } from "@/types/admin";

export function useAdminUsers(params: AdminUserSearchParams) {
  return useQuery({
    queryKey: ["admin", "users", params] as const,
    queryFn: () => listPlatformUsers(params),
  });
}

export function useEnableUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => enablePlatformUser(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
  });
}

export function useDisableUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => disablePlatformUser(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
  });
}
