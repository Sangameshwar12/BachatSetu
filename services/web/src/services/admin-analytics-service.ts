import { apiClient } from "@/services/api-client";
import type {
  GroupAnalyticsResponse,
  NotificationAnalyticsResponse,
  OverviewAnalyticsResponse,
  PaymentAnalyticsResponse,
  StorageAnalyticsResponse,
  UserAnalyticsResponse,
} from "@/types/admin-analytics";

const BASE = "/api/v1/admin/analytics";

/** `GET /api/v1/admin/analytics/overview` */
export async function getOverviewAnalytics(): Promise<OverviewAnalyticsResponse> {
  const { data } = await apiClient.get<OverviewAnalyticsResponse>(`${BASE}/overview`);
  return data;
}

/** `GET /api/v1/admin/analytics/payments` — includes a 30-day trend. */
export async function getPaymentAnalytics(): Promise<PaymentAnalyticsResponse> {
  const { data } = await apiClient.get<PaymentAnalyticsResponse>(`${BASE}/payments`);
  return data;
}

/** `GET /api/v1/admin/analytics/groups` */
export async function getGroupAnalytics(): Promise<GroupAnalyticsResponse> {
  const { data } = await apiClient.get<GroupAnalyticsResponse>(`${BASE}/groups`);
  return data;
}

/** `GET /api/v1/admin/analytics/users` */
export async function getUserAnalytics(): Promise<UserAnalyticsResponse> {
  const { data } = await apiClient.get<UserAnalyticsResponse>(`${BASE}/users`);
  return data;
}

/** `GET /api/v1/admin/analytics/notifications` */
export async function getNotificationAnalytics(): Promise<NotificationAnalyticsResponse> {
  const { data } = await apiClient.get<NotificationAnalyticsResponse>(`${BASE}/notifications`);
  return data;
}

/** `GET /api/v1/admin/analytics/storage` — includes a 30-day upload trend. */
export async function getStorageAnalytics(): Promise<StorageAnalyticsResponse> {
  const { data } = await apiClient.get<StorageAnalyticsResponse>(`${BASE}/storage`);
  return data;
}
