/** `GET/PUT /api/v1/admin/config` — the platform configuration singleton. */
export interface PlatformConfigurationResponse {
  defaultLanguage: string;
  otpExpirySeconds: number;
  defaultStorageProvider: string;
  defaultPaymentProvider: string;
  notificationRetryCount: number;
  maximumUploadSizeBytes: number;
  maximumMembersPerGroup: number;
  maximumGroupsPerOrganizer: number;
  maintenanceEnabled: boolean;
  maintenanceMessage: string | null;
  maintenanceStartAt: string | null;
  maintenanceEndAt: string | null;
  version: number;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface UpdateConfigurationRequest {
  defaultLanguage: string;
  otpExpirySeconds: number;
  defaultStorageProvider: string;
  defaultPaymentProvider: string;
  notificationRetryCount: number;
  maximumUploadSizeBytes: number;
  maximumMembersPerGroup: number;
  maximumGroupsPerOrganizer: number;
  maintenanceEnabled: boolean;
  maintenanceMessage: string | null;
  maintenanceStartAt: string | null;
  maintenanceEndAt: string | null;
}

/** One row of `GET /api/v1/admin/config/feature-flags`. */
export interface FeatureFlagResponse {
  key: string;
  enabled: boolean;
  version: number;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface UpdateFeatureFlagsRequest {
  flags: Record<string, boolean>;
}

/** One row of `GET /api/v1/admin/config/limits`. */
export interface PlatformLimitResponse {
  key: string;
  value: number;
  version: number;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface UpdateSystemLimitsRequest {
  limits: Record<string, number>;
}
