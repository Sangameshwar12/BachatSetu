import { apiClient } from "@/services/api-client";
import type {
  AnnouncementResponse,
  BroadcastRequest,
  BroadcastResponse,
  PlatformOverviewResponse,
  PublishAnnouncementRequest,
  SuspendTenantRequest,
  SystemHealthResponse,
  TenantResponse,
} from "@/types/platform-operations";
import type { Page } from "@/types/pagination";

const BASE = "/api/v1/platform-operations";

/** `GET /api/v1/platform-operations/overview` — platform-wide totals plus today's activity. */
export async function getPlatformOverview(): Promise<PlatformOverviewResponse> {
  const { data } = await apiClient.get<PlatformOverviewResponse>(`${BASE}/overview`);
  return data;
}

/** `GET /api/v1/platform-operations/tenants` — every known tenant, with per-tenant statistics. */
export async function searchTenants(
  status?: string,
  page?: number,
  size?: number
): Promise<Page<TenantResponse>> {
  const { data } = await apiClient.get<Page<TenantResponse>>(`${BASE}/tenants`, {
    params: { status, page, size },
  });
  return data;
}

/** `POST /api/v1/platform-operations/tenants/{tenantId}/suspend` */
export async function suspendTenant(tenantId: string, request: SuspendTenantRequest): Promise<TenantResponse> {
  const { data } = await apiClient.post<TenantResponse>(`${BASE}/tenants/${tenantId}/suspend`, request);
  return data;
}

/** `POST /api/v1/platform-operations/tenants/{tenantId}/activate` */
export async function activateTenant(tenantId: string): Promise<TenantResponse> {
  const { data } = await apiClient.post<TenantResponse>(`${BASE}/tenants/${tenantId}/activate`);
  return data;
}

/** `POST /api/v1/platform-operations/tenants/{tenantId}/archive` */
export async function archiveTenant(tenantId: string): Promise<TenantResponse> {
  const { data } = await apiClient.post<TenantResponse>(`${BASE}/tenants/${tenantId}/archive`);
  return data;
}

/** `GET /api/v1/platform-operations/health` — database/storage/notification health plus JVM/host facts. */
export async function getSystemHealth(): Promise<SystemHealthResponse> {
  const { data } = await apiClient.get<SystemHealthResponse>(`${BASE}/health`);
  return data;
}

/** `POST /api/v1/platform-operations/broadcast` — sends a broadcast notification, reusing the Notification module. */
export async function sendBroadcast(request: BroadcastRequest): Promise<BroadcastResponse> {
  const { data } = await apiClient.post<BroadcastResponse>(`${BASE}/broadcast`, request);
  return data;
}

/** `GET /api/v1/platform-operations/announcements` — every announcement, platform admin only. */
export async function listAnnouncements(page?: number, size?: number): Promise<Page<AnnouncementResponse>> {
  const { data } = await apiClient.get<Page<AnnouncementResponse>>(`${BASE}/announcements`, {
    params: { page, size },
  });
  return data;
}

/** `POST /api/v1/platform-operations/announcements` */
export async function publishAnnouncement(request: PublishAnnouncementRequest): Promise<AnnouncementResponse> {
  const { data } = await apiClient.post<AnnouncementResponse>(`${BASE}/announcements`, request);
  return data;
}
