/** `GET /api/v1/admin/statistics` response. */
export interface PlatformStatisticsResponse {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  totalGroups: number;
  activeGroups: number;
  totalPayments: number;
  completedPayments: number;
  totalReceipts: number;
  totalNotifications: number;
  totalFiles: number;
}

export type PlatformUserStatus = "PENDING_VERIFICATION" | "ACTIVE" | "LOCKED" | "SUSPENDED" | "DISABLED";

/** One row of `GET /api/v1/admin/users`. */
export interface PlatformUserResponse {
  userId: string;
  tenantId: string;
  email: string | null;
  phoneNumber: string | null;
  firstName: string | null;
  lastName: string | null;
  status: PlatformUserStatus;
  createdAt: string;
}

export type PlatformGroupStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "CLOSED";

/** One row of `GET /api/v1/admin/groups`. */
export interface PlatformGroupResponse {
  groupId: string;
  tenantId: string;
  code: string;
  name: string;
  status: PlatformGroupStatus;
  memberCount: number;
  createdAt: string;
}

/** One row of `GET /api/v1/admin/tenants`. */
export interface PlatformTenantResponse {
  tenantId: string;
  userCount: number;
  groupCount: number;
}

export interface AdminUserSearchParams {
  status?: string;
  email?: string;
  phone?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export interface AdminGroupSearchParams {
  status?: string;
  page?: number;
  size?: number;
  direction?: "asc" | "desc";
}
