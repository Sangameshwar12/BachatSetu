import { apiClient } from "@/services/api-client";
import type {
  FeatureFlagResponse,
  PlatformConfigurationResponse,
  PlatformLimitResponse,
  UpdateConfigurationRequest,
  UpdateFeatureFlagsRequest,
  UpdateSystemLimitsRequest,
} from "@/types/admin-config";

const BASE = "/api/v1/admin/config";

/** `GET /api/v1/admin/config` — current platform-wide settings and maintenance-mode state. */
export async function getConfiguration(): Promise<PlatformConfigurationResponse> {
  const { data } = await apiClient.get<PlatformConfigurationResponse>(BASE);
  return data;
}

/** `PUT /api/v1/admin/config` — full-replace update. */
export async function updateConfiguration(
  request: UpdateConfigurationRequest
): Promise<PlatformConfigurationResponse> {
  const { data } = await apiClient.put<PlatformConfigurationResponse>(BASE, request);
  return data;
}

/** `GET /api/v1/admin/config/feature-flags` */
export async function getFeatureFlags(): Promise<FeatureFlagResponse[]> {
  const { data } = await apiClient.get<FeatureFlagResponse[]>(`${BASE}/feature-flags`);
  return data;
}

/** `PUT /api/v1/admin/config/feature-flags` — partial update: only keys present in `flags` change. */
export async function updateFeatureFlags(request: UpdateFeatureFlagsRequest): Promise<FeatureFlagResponse[]> {
  const { data } = await apiClient.put<FeatureFlagResponse[]>(`${BASE}/feature-flags`, request);
  return data;
}

/** `GET /api/v1/admin/config/limits` */
export async function getSystemLimits(): Promise<PlatformLimitResponse[]> {
  const { data } = await apiClient.get<PlatformLimitResponse[]>(`${BASE}/limits`);
  return data;
}

/** `PUT /api/v1/admin/config/limits` — partial update: only keys present in `limits` change. */
export async function updateSystemLimits(request: UpdateSystemLimitsRequest): Promise<PlatformLimitResponse[]> {
  const { data } = await apiClient.put<PlatformLimitResponse[]>(`${BASE}/limits`, request);
  return data;
}
