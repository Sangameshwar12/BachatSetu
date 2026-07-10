"use client";

import { useQuery } from "@tanstack/react-query";

import { getOrganizerDashboard } from "@/services/organizer-dashboard-service";

export const organizerDashboardQueryKey = ["dashboard", "organizer"] as const;

export function useOrganizerDashboard() {
  return useQuery({
    queryKey: organizerDashboardQueryKey,
    queryFn: getOrganizerDashboard,
  });
}
