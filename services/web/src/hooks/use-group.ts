"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { organizerDashboardQueryKey } from "@/hooks/use-organizer-dashboard";
import { memberDashboardQueryKey } from "@/hooks/use-member-dashboard";
import { createGroup, getGroup } from "@/services/group-service";
import type { CreateSavingsGroupRequest } from "@/types/group";

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: ["group", groupId] as const,
    queryFn: () => getGroup(groupId),
    enabled: Boolean(groupId),
  });
}

export function useCreateGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateSavingsGroupRequest) => createGroup(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: memberDashboardQueryKey });
      queryClient.invalidateQueries({ queryKey: organizerDashboardQueryKey });
    },
  });
}
