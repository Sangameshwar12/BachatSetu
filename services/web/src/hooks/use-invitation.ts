"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { joinGroup, previewInvitation } from "@/services/invitation-service";
import type { AcceptInvitationRequest } from "@/types/invitation";
import { memberDashboardQueryKey } from "@/hooks/use-member-dashboard";

export function useInvitationPreview(token: string) {
  return useQuery({
    queryKey: ["invitation", "preview", token] as const,
    queryFn: () => previewInvitation(token),
    enabled: Boolean(token),
    retry: false,
  });
}

export function useJoinGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AcceptInvitationRequest) => joinGroup(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: memberDashboardQueryKey });
    },
  });
}
