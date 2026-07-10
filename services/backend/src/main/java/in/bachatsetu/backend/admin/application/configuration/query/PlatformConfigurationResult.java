package in.bachatsetu.backend.admin.application.configuration.query;

import java.time.Instant;
import java.util.UUID;

/** Application-layer read model mirroring {@code PlatformConfiguration}. */
public record PlatformConfigurationResult(
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
        Instant maintenanceStartAt,
        Instant maintenanceEndAt,
        long version,
        Instant updatedAt,
        UUID updatedBy) {
}
