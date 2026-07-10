"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  getConfiguration,
  getFeatureFlags,
  getSystemLimits,
  updateConfiguration,
  updateFeatureFlags,
  updateSystemLimits,
} from "@/services/admin-config-service";
import type {
  UpdateConfigurationRequest,
  UpdateFeatureFlagsRequest,
  UpdateSystemLimitsRequest,
} from "@/types/admin-config";

export function useAdminConfiguration() {
  return useQuery({ queryKey: ["admin", "config"] as const, queryFn: getConfiguration });
}

export function useUpdateAdminConfiguration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateConfigurationRequest) => updateConfiguration(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "config"] }),
  });
}

export function useFeatureFlags() {
  return useQuery({ queryKey: ["admin", "config", "feature-flags"] as const, queryFn: getFeatureFlags });
}

export function useUpdateFeatureFlags() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateFeatureFlagsRequest) => updateFeatureFlags(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "config", "feature-flags"] }),
  });
}

export function useSystemLimits() {
  return useQuery({ queryKey: ["admin", "config", "limits"] as const, queryFn: getSystemLimits });
}

export function useUpdateSystemLimits() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateSystemLimitsRequest) => updateSystemLimits(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "config", "limits"] }),
  });
}
