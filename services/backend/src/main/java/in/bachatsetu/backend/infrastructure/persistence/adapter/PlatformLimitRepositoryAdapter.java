package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.config.PlatformLimitJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PlatformLimitSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class PlatformLimitRepositoryAdapter implements PlatformLimitRepository {

    private final PlatformLimitSpringDataRepository repository;

    public PlatformLimitRepositoryAdapter(PlatformLimitSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<PlatformLimit> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PlatformLimit> findByKey(LimitKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return repository.findById(key).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(PlatformLimit limit) {
        Objects.requireNonNull(limit, "limit must not be null");
        PlatformLimitJpaEntity entity = repository.findById(limit.key())
                .orElseGet(() -> new PlatformLimitJpaEntity(limit.key(), limit.value(), limit.version(),
                        limit.updatedAt(), toUuid(limit.updatedBy())));
        entity.update(limit.value(), limit.version(), limit.updatedAt(), toUuid(limit.updatedBy()));
        repository.save(entity);
    }

    private PlatformLimit toDomain(PlatformLimitJpaEntity entity) {
        AggregateId updatedBy = entity.getUpdatedBy() == null ? null : new AggregateId(entity.getUpdatedBy());
        return new PlatformLimit(
                entity.getLimitKey(), entity.getLimitValue(), entity.getVersion(), entity.getUpdatedAt(), updatedBy);
    }

    private UUID toUuid(AggregateId aggregateId) {
        return aggregateId == null ? null : aggregateId.value();
    }
}
