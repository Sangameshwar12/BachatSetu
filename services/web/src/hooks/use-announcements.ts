"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listAnnouncements, publishAnnouncement } from "@/services/platform-operations-service";
import type { PublishAnnouncementRequest } from "@/types/platform-operations";

export function useAnnouncements(page: number, size: number) {
  return useQuery({
    queryKey: ["platform-operations", "announcements", { page, size }] as const,
    queryFn: () => listAnnouncements(page, size),
  });
}

export function usePublishAnnouncement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: PublishAnnouncementRequest) => publishAnnouncement(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["platform-operations", "announcements"] }),
  });
}
