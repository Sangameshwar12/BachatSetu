package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.UpdatableDomainMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractJpaRepositoryAdapter<D, E extends BaseJpaEntity> {

    private final BaseJpaRepository<E> repository;
    private final UpdatableDomainMapper<D, E> mapper;

    protected AbstractJpaRepositoryAdapter(
            BaseJpaRepository<E> repository,
            UpdatableDomainMapper<D, E> mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    protected final Optional<D> findDomainById(AggregateId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return repository.findByIdAndDeletedFalse(aggregateId.value()).map(mapper::toDomain);
    }

    protected final D insert(D domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        E savedEntity = repository.save(mapper.toEntity(domain));
        return mapper.toDomain(savedEntity);
    }

    protected final D update(D domain, E entity) {
        Objects.requireNonNull(domain, "domain must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        mapper.updateEntity(domain, entity);
        return mapper.toDomain(repository.save(entity));
    }

    protected final BaseJpaRepository<E> repository() {
        return repository;
    }
}
