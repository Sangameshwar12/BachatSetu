package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.TenantJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;

public interface TenantSpringDataRepository extends BaseJpaRepository<TenantJpaEntity> {

    long countByStatusAndDeletedFalse(TenantStatus status);
}
