package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GroupSpringDataRepository extends BaseJpaRepository<GroupJpaEntity> {

    Optional<GroupJpaEntity> findByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
