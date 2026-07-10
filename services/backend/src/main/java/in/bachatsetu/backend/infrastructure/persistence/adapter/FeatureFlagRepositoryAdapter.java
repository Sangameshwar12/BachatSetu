package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.FeatureFlagJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.FeatureFlagSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class FeatureFlagRepositoryAdapter implements FeatureFlagRepository {

    private final FeatureFlagSpringDataRepository repository;

    public FeatureFlagRepositoryAdapter(FeatureFlagSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<FeatureFlag> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FeatureFlag> findByKey(FeatureKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return repository.findById(key).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(FeatureFlag flag) {
        Objects.requireNonNull(flag, "flag must not be null");
        FeatureFlagJpaEntity entity = repository.findById(flag.key())
                .orElseGet(() -> new FeatureFlagJpaEntity(flag.key(), flag.enabled(), flag.version(),
                        flag.updatedAt(), toUuid(flag.updatedBy())));
        entity.update(flag.enabled(), flag.version(), flag.updatedAt(), toUuid(flag.updatedBy()));
        repository.save(entity);
    }

    private FeatureFlag toDomain(FeatureFlagJpaEntity entity) {
        AggregateId updatedBy = entity.getUpdatedBy() == null ? null : new AggregateId(entity.getUpdatedBy());
        return new FeatureFlag(entity.getFeatureKey(), entity.isEnabled(), entity.getVersion(), entity.getUpdatedAt(),
                updatedBy);
    }

    private java.util.UUID toUuid(AggregateId aggregateId) {
        return aggregateId == null ? null : aggregateId.value();
    }
}
