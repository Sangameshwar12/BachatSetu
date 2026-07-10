/** `GET /api/v1/platform-operations/overview` — Super Admin Dashboard totals + today's activity. */
export interface PlatformOverviewResponse {
  totalUsers: number;
  totalOrganizers: number;
  totalGroups: number;
  totalMembers: number;
  totalPayments: number;
  totalReceipts: number;
  totalNotifications: number;
  totalStoredFiles: number;
  totalActiveTenants: number;
  totalRevenuePaise: number;
  todaySignups: number;
  todayPayments: number;
  todayGroups: number;
  todayNotifications: number;
  todayStorageUploads: number;
}

export type TenantStatus = "ACTIVE" | "SUSPENDED" | "ARCHIVED";

/** One row of `GET /api/v1/platform-operations/tenants`. */
export interface TenantResponse {
  tenantId: string;
  status: TenantStatus;
  suspensionReason: string | null;
  users: number;
  groups: number;
  payments: number;
  revenuePaise: number;
  storageFiles: number;
  storageBytes: number;
  notifications: number;
  lastActivityAt: string | null;
}

export interface SuspendTenantRequest {
  reason: string;
}

export interface ComponentHealth {
  name: string;
  status: string;
  detail: string | null;
}

/** `GET /api/v1/platform-operations/health` */
export interface SystemHealthResponse {
  database: ComponentHealth;
  storage: ComponentHealth;
  notification: ComponentHealth;
  uptimeSeconds: number;
  javaVersion: string;
  applicationVersion: string;
  buildTimestamp: string | null;
  usedMemoryBytes: number;
  totalMemoryBytes: number;
  maxMemoryBytes: number;
  usableDiskBytes: number;
  totalDiskBytes: number;
}

export type BroadcastScope = "ALL_USERS" | "TENANT" | "ORGANIZERS" | "MEMBERS";

export interface BroadcastRequest {
  scope: BroadcastScope;
  tenantId?: string;
  title: string;
  message: string;
}

export interface BroadcastResponse {
  recipientCount: number;
  sentCount: number;
  failedCount: number;
}

export type AnnouncementSeverity = "INFO" | "WARNING" | "CRITICAL";

export interface AnnouncementResponse {
  announcementId: string;
  title: string;
  message: string;
  startAt: string;
  endAt: string;
  severity: AnnouncementSeverity;
  active: boolean;
}

export interface PublishAnnouncementRequest {
  title: string;
  message: string;
  startAt: string;
  endAt: string;
  severity: AnnouncementSeverity;
}
