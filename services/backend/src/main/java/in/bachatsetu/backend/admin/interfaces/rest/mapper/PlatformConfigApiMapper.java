package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateConfigurationCommand;
import in.bachatsetu.backend.admin.application.configuration.command.UpdateFeatureFlagsCommand;
import in.bachatsetu.backend.admin.application.configuration.command.UpdateSystemLimitsCommand;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.FeatureFlagResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.PlatformConfigurationResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.PlatformLimitResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateConfigurationRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateFeatureFlagsRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateSystemLimitsRequest;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps platform configuration application-layer commands and read models to and from safe REST shapes. */
@Component
public class PlatformConfigApiMapper {

    public PlatformConfigurationResponse toResponse(PlatformConfigurationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformConfigurationResponse(
                result.defaultLanguage(),
                result.otpExpirySeconds(),
                result.defaultStorageProvider(),
                result.defaultPaymentProvider(),
                result.notificationRetryCount(),
                result.maximumUploadSizeBytes(),
                result.maximumMembersPerGroup(),
                result.maximumGroupsPerOrganizer(),
                result.maintenanceEnabled(),
                result.maintenanceMessage(),
                toString(result.maintenanceStartAt()),
                toString(result.maintenanceEndAt()),
                result.version(),
                toString(result.updatedAt()),
                result.updatedBy() == null ? null : result.updatedBy().toString());
    }

    public UpdateConfigurationCommand toCommand(UpdateConfigurationRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        return new UpdateConfigurationCommand(
                currentUser.userId().toAggregateId(),
                request.defaultLanguage(),
                request.otpExpirySeconds(),
                request.defaultStorageProvider(),
                request.defaultPaymentProvider(),
                request.notificationRetryCount(),
                request.maximumUploadSizeBytes(),
                request.maximumMembersPerGroup(),
                request.maximumGroupsPerOrganizer(),
                request.maintenanceEnabled(),
                request.maintenanceMessage(),
                toInstant(request.maintenanceStartAt()),
                toInstant(request.maintenanceEndAt()));
    }

    public FeatureFlagResponse toResponse(FeatureFlagResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new FeatureFlagResponse(
                result.key(), result.enabled(), result.version(), toString(result.updatedAt()),
                result.updatedBy() == null ? null : result.updatedBy().toString());
    }

    public UpdateFeatureFlagsCommand toCommand(UpdateFeatureFlagsRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        Map<FeatureKey, Boolean> changes = new LinkedHashMap<>();
        request.flags().forEach((key, enabled) -> changes.put(toFeatureKey(key), enabled));
        return new UpdateFeatureFlagsCommand(currentUser.userId().toAggregateId(), changes);
    }

    public PlatformLimitResponse toResponse(PlatformLimitResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformLimitResponse(
                result.key(), result.value(), result.version(), toString(result.updatedAt()),
                result.updatedBy() == null ? null : result.updatedBy().toString());
    }

    public UpdateSystemLimitsCommand toCommand(UpdateSystemLimitsRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        Map<LimitKey, Long> changes = new LinkedHashMap<>();
        request.limits().forEach((key, value) -> changes.put(toLimitKey(key), value));
        return new UpdateSystemLimitsCommand(currentUser.userId().toAggregateId(), changes);
    }

    private FeatureKey toFeatureKey(String key) {
        try {
            return FeatureKey.valueOf(key);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown feature key: " + key, exception);
        }
    }

    private LimitKey toLimitKey(String key) {
        try {
            return LimitKey.valueOf(key);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown limit key: " + key, exception);
        }
    }

    private String toString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private Instant toInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid instant: " + value, exception);
        }
    }
}
