"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { organizerDashboardQueryKey } from "@/hooks/use-organizer-dashboard";
import { getCollectionSummary, markMemberPaid } from "@/services/collection-service";

export function collectionSummaryQueryKey(groupId: string) {
  return ["group", groupId, "collection"] as const;
}

export function useCollectionSummary(groupId: string) {
  return useQuery({
    queryKey: collectionSummaryQueryKey(groupId),
    queryFn: () => getCollectionSummary(groupId),
    enabled: Boolean(groupId),
  });
}

export function useMarkMemberPaid(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (memberId: string) => markMemberPaid(groupId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: collectionSummaryQueryKey(groupId) });
      queryClient.invalidateQueries({ queryKey: organizerDashboardQueryKey });
    },
  });
}
