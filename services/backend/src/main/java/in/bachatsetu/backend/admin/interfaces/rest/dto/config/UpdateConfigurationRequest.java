package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Full-replace update of the platform configuration singleton.")
public record UpdateConfigurationRequest(
        @NotBlank String defaultLanguage,
        @Positive int otpExpirySeconds,
        @NotBlank String defaultStorageProvider,
        @NotBlank String defaultPaymentProvider,
        @PositiveOrZero int notificationRetryCount,
        @Positive long maximumUploadSizeBytes,
        @Positive int maximumMembersPerGroup,
        @Positive int maximumGroupsPerOrganizer,
        boolean maintenanceEnabled,
        String maintenanceMessage,
        String maintenanceStartAt,
        String maintenanceEndAt) {
}
