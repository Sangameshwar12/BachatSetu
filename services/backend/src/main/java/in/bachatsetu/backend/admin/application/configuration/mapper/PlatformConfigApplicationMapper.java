package in.bachatsetu.backend.admin.application.configuration.mapper;

import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/** Converts platform configuration domain state into application-layer read models. */
public final class PlatformConfigApplicationMapper {

    public PlatformConfigurationResult toResult(PlatformConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        return new PlatformConfigurationResult(
                configuration.defaultLanguage(),
                configuration.otpExpirySeconds(),
                configuration.defaultStorageProvider(),
                configuration.defaultPaymentProvider(),
                configuration.notificationRetryCount(),
                configuration.maximumUploadSizeBytes(),
                configuration.maximumMembersPerGroup(),
                configuration.maximumGroupsPerOrganizer(),
                configuration.maintenanceEnabled(),
                configuration.maintenanceMessage(),
                configuration.maintenanceStartAt(),
                configuration.maintenanceEndAt(),
                configuration.version(),
                configuration.updatedAt(),
                toUuid(configuration.updatedBy()));
    }

    public FeatureFlagResult toResult(FeatureFlag flag) {
        Objects.requireNonNull(flag, "flag must not be null");
        return new FeatureFlagResult(
                flag.key().name(), flag.enabled(), flag.version(), flag.updatedAt(), toUuid(flag.updatedBy()));
    }

    public PlatformLimitResult toResult(PlatformLimit limit) {
        Objects.requireNonNull(limit, "limit must not be null");
        return new PlatformLimitResult(
                limit.key().name(), limit.value(), limit.version(), limit.updatedAt(), toUuid(limit.updatedBy()));
    }

    private UUID toUuid(AggregateId aggregateId) {
        return aggregateId == null ? null : aggregateId.value();
    }
}
