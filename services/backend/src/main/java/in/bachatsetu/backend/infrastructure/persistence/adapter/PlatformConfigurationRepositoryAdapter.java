package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformConfigurationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PlatformConfigurationSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Loads and persists the single platform configuration row, seeded by {@code V11__platform_configuration.sql}. */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class PlatformConfigurationRepositoryAdapter implements PlatformConfigurationRepository {

    private static final short SINGLETON_ID = 1;

    private final PlatformConfigurationSpringDataRepository repository;

    public PlatformConfigurationRepositoryAdapter(PlatformConfigurationSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PlatformConfiguration find() {
        PlatformConfigurationJpaEntity entity = repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "platform configuration row is missing; V11__platform_configuration.sql may not have run"));
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void save(PlatformConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        PlatformConfigurationJpaEntity entity = repository.findById(SINGLETON_ID)
                .orElseGet(PlatformConfigurationJpaEntity::new);
        entity.update(
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
                configuration.updatedBy() == null ? null : configuration.updatedBy().value());
        repository.save(entity);
    }

    private PlatformConfiguration toDomain(PlatformConfigurationJpaEntity entity) {
        return PlatformConfiguration.of(
                entity.getDefaultLanguage(),
                entity.getOtpExpirySeconds(),
                entity.getDefaultStorageProvider(),
                entity.getDefaultPaymentProvider(),
                entity.getNotificationRetryCount(),
                entity.getMaximumUploadSizeBytes(),
                entity.getMaximumMembersPerGroup(),
                entity.getMaximumGroupsPerOrganizer(),
                entity.isMaintenanceEnabled(),
                entity.getMaintenanceMessage(),
                entity.getMaintenanceStartAt(),
                entity.getMaintenanceEndAt(),
                entity.getVersion(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy() == null ? null : new AggregateId(entity.getUpdatedBy()));
    }
}
