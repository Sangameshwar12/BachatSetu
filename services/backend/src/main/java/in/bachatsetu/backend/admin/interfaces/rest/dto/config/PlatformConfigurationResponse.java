package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The platform-wide configuration singleton: general settings plus maintenance-mode state.")
public record PlatformConfigurationResponse(
        String defaultLanguage,
        int otpExpirySeconds,
        String defaultStorageProvider,
        String defaultPaymentProvider,
        int notificationRetryCount,
        long maximumUploadSizeBytes,
        int maximumMembersPerGroup,
        int maximumGroupsPerOrganizer,
        boolean maintenanceEnabled,
        String maintenanceMessage,
        String maintenanceStartAt,
        String maintenanceEndAt,
        long version,
        String updatedAt,
        String updatedBy) {
}
