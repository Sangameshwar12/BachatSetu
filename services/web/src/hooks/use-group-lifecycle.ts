"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { activateGroup, closeGroup, suspendGroup } from "@/services/group-service";

/** Organizer-only lifecycle transitions for a savings group. */
export function useGroupLifecycle(groupId: string) {
  const queryClient = useQueryClient();

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    queryClient.invalidateQueries({ queryKey: ["dashboard", "organizer"] });
  }

  const activate = useMutation({
    mutationFn: () => activateGroup(groupId),
    onSuccess: invalidate,
  });
  const suspend = useMutation({
    mutationFn: () => suspendGroup(groupId),
    onSuccess: invalidate,
  });
  const close = useMutation({
    mutationFn: () => closeGroup(groupId),
    onSuccess: invalidate,
  });

  return { activate, suspend, close };
}
