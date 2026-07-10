import { apiClient } from "@/services/api-client";
import type {
  AdminGroupSearchParams,
  AdminUserSearchParams,
  PlatformGroupResponse,
  PlatformStatisticsResponse,
  PlatformTenantResponse,
  PlatformUserResponse,
} from "@/types/admin";
import type { Page } from "@/types/pagination";

/** `GET /api/v1/admin/statistics` — platform-wide totals, computed on demand. Platform admin only. */
export async function getPlatformStatistics(): Promise<PlatformStatisticsResponse> {
  const { data } = await apiClient.get<PlatformStatisticsResponse>("/api/v1/admin/statistics");
  return data;
}

/** `GET /api/v1/admin/users` — searches users across every tenant, page by page. */
export async function listPlatformUsers(params: AdminUserSearchParams): Promise<Page<PlatformUserResponse>> {
  const { data } = await apiClient.get<Page<PlatformUserResponse>>("/api/v1/admin/users", { params });
  return data;
}

/** `GET /api/v1/admin/groups` — searches savings groups across every tenant, page by page. */
export async function listPlatformGroups(params: AdminGroupSearchParams): Promise<Page<PlatformGroupResponse>> {
  const { data } = await apiClient.get<Page<PlatformGroupResponse>>("/api/v1/admin/groups", { params });
  return data;
}

/** `GET /api/v1/admin/tenants` — tenants known to the platform, with per-tenant totals. */
export async function listPlatformTenants(page?: number, size?: number): Promise<Page<PlatformTenantResponse>> {
  const { data } = await apiClient.get<Page<PlatformTenantResponse>>("/api/v1/admin/tenants", {
    params: { page, size },
  });
  return data;
}

/** `POST /api/v1/admin/users/{id}/enable` */
export async function enablePlatformUser(userId: string): Promise<PlatformUserResponse> {
  const { data } = await apiClient.post<PlatformUserResponse>(`/api/v1/admin/users/${userId}/enable`);
  return data;
}

/** `POST /api/v1/admin/users/{id}/disable` */
export async function disablePlatformUser(userId: string): Promise<PlatformUserResponse> {
  const { data } = await apiClient.post<PlatformUserResponse>(`/api/v1/admin/users/${userId}/disable`);
  return data;
}
