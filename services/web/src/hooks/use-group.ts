"use client";

import { useQuery } from "@tanstack/react-query";

import { getGroup } from "@/services/group-service";

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: ["group", groupId] as const,
    queryFn: () => getGroup(groupId),
    enabled: Boolean(groupId),
  });
}
