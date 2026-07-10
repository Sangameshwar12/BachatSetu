export interface DistributionEntry {
  key: string;
  count: number;
}

export interface MonthlyMetric {
  month: string;
  count: number;
}

export interface PaymentTrendPoint {
  date: string;
  count: number;
  volumePaise: number;
}

export interface UploadTrendPoint {
  date: string;
  count: number;
}

export interface TenantUserCount {
  tenantId: string;
  userCount: number;
}

/** `GET /api/v1/admin/analytics/overview` */
export interface OverviewAnalyticsResponse {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  totalTenants: number;
  totalGroups: number;
  activeGroups: number;
  completedGroups: number;
  totalPayments: number;
  verifiedPayments: number;
  failedPayments: number;
  totalReceipts: number;
  totalNotifications: number;
  totalStoredFiles: number;
}

/** `GET /api/v1/admin/analytics/payments` */
export interface PaymentAnalyticsResponse {
  totalPaymentVolumePaise: number;
  verifiedPaymentVolumePaise: number;
  failedPaymentCount: number;
  pendingPaymentCount: number;
  averageContributionPaise: number;
  paymentSuccessRate: number;
  paymentFailureRate: number;
  paymentTrend: PaymentTrendPoint[];
}

/** `GET /api/v1/admin/analytics/groups` */
export interface GroupAnalyticsResponse {
  totalGroups: number;
  activeGroups: number;
  completedGroups: number;
  averageMembersPerGroup: number;
  averageContributionAmountPaise: number;
  monthlyNewGroups: MonthlyMetric[];
  drawCompletionRate: number;
}

/** `GET /api/v1/admin/analytics/users` */
export interface UserAnalyticsResponse {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  monthlyRegistrations: MonthlyMetric[];
  preferredLanguageDistribution: DistributionEntry[];
  usersPerTenant: TenantUserCount[];
}

/** `GET /api/v1/admin/analytics/notifications` */
export interface NotificationAnalyticsResponse {
  totalNotifications: number;
  unreadNotifications: number;
  deliveryStatusCounts: DistributionEntry[];
  notificationTypeDistribution: DistributionEntry[];
}

/** `GET /api/v1/admin/analytics/storage` */
export interface StorageAnalyticsResponse {
  totalFiles: number;
  totalStorageBytes: number;
  averageFileSizeBytes: number;
  storageProviderDistribution: DistributionEntry[];
  uploadsPerDay: UploadTrendPoint[];
}
