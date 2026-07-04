package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;

public interface PermissionSpringDataRepository extends BaseJpaRepository<PermissionJpaEntity> {

    Optional<PermissionJpaEntity> findByCodeAndDeletedFalse(String code);
}
