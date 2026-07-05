package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RoleSpringDataRepository extends BaseJpaRepository<RoleJpaEntity> {

    Optional<RoleJpaEntity> findByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);

    Optional<RoleJpaEntity> findByTenantIdIsNullAndCodeAndDeletedFalse(String code);
}
