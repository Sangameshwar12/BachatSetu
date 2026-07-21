"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { organizerDashboardQueryKey } from "@/hooks/use-organizer-dashboard";
import { memberDashboardQueryKey } from "@/hooks/use-member-dashboard";
import { createGroup, getGroup, removeMember } from "@/services/group-service";
import type { CreateSavingsGroupRequest } from "@/types/group";

export function groupQueryKey(groupId: string) {
  return ["group", groupId] as const;
}

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: groupQueryKey(groupId),
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

export function useRemoveMember(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (memberId: string) => removeMember(groupId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: groupQueryKey(groupId) });
      queryClient.invalidateQueries({ queryKey: organizerDashboardQueryKey });
    },
  });
}
