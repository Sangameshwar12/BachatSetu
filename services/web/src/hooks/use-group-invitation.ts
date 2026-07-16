"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/services/api-client";
import { createInvitation, getCurrentInvitation, revokeInvitation } from "@/services/invitation-service";
import type { CreateInvitationRequest } from "@/types/invitation";

export function invitationQueryKey(groupId: string) {
  return ["group-invitation", groupId] as const;
}

export function useCurrentInvitation(groupId: string) {
  return useQuery({
    queryKey: invitationQueryKey(groupId),
    queryFn: () => getCurrentInvitation(groupId),
    enabled: Boolean(groupId),
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.code === "no-active-invitation") {
        return false;
      }
      return failureCount < 1;
    },
  });
}

/** True when the query failed specifically because the group has no active invitation yet. */
export function isNoActiveInvitationError(error: unknown): boolean {
  return error instanceof ApiError && error.code === "no-active-invitation";
}

export function useCreateInvitation(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateInvitationRequest) => createInvitation(groupId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invitationQueryKey(groupId) });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "organizer"] });
    },
  });
}

export function useRevokeInvitation(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => revokeInvitation(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invitationQueryKey(groupId) });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "organizer"] });
    },
  });
}
