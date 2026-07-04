package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;

public interface DomainMapper<D, E extends BaseJpaEntity> {

    D toDomain(E entity);

    E toEntity(D domain);
}
