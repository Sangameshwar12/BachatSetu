package in.bachatsetu.backend.admin.application.configuration.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Full-replace update of the platform configuration singleton, including maintenance-mode state. */
public record UpdateConfigurationCommand(
        AggregateId administratorId,
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
        Instant maintenanceEndAt) {

    public UpdateConfigurationCommand {
        Objects.requireNonNull(administratorId, "administratorId must not be null");
    }
}
