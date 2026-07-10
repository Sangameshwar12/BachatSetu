"use client";

import { useQuery } from "@tanstack/react-query";

import {
  getGroupAnalytics,
  getNotificationAnalytics,
  getOverviewAnalytics,
  getPaymentAnalytics,
  getStorageAnalytics,
  getUserAnalytics,
} from "@/services/admin-analytics-service";

export function useOverviewAnalytics() {
  return useQuery({ queryKey: ["admin", "analytics", "overview"] as const, queryFn: getOverviewAnalytics });
}

export function usePaymentAnalytics() {
  return useQuery({ queryKey: ["admin", "analytics", "payments"] as const, queryFn: getPaymentAnalytics });
}

export function useGroupAnalytics() {
  return useQuery({ queryKey: ["admin", "analytics", "groups"] as const, queryFn: getGroupAnalytics });
}

export function useUserAnalytics() {
  return useQuery({ queryKey: ["admin", "analytics", "users"] as const, queryFn: getUserAnalytics });
}

export function useNotificationAnalytics() {
  return useQuery({
    queryKey: ["admin", "analytics", "notifications"] as const,
    queryFn: getNotificationAnalytics,
  });
}

export function useStorageAnalytics() {
  return useQuery({ queryKey: ["admin", "analytics", "storage"] as const, queryFn: getStorageAnalytics });
}
