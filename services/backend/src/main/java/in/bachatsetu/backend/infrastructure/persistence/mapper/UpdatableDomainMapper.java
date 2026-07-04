package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import org.mapstruct.MappingTarget;

public interface UpdatableDomainMapper<D, E extends BaseJpaEntity> extends DomainMapper<D, E> {

    void updateEntity(D domain, @MappingTarget E entity);
}
