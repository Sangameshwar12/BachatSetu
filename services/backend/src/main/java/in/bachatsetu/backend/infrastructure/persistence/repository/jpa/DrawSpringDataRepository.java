package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DrawSpringDataRepository extends BaseJpaRepository<DrawJpaEntity> {

    Optional<DrawJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    Page<DrawJpaEntity> findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<DrawJpaEntity> findByGroup_IdAndCycle_CycleNumberAndDeletedFalse(
            UUID groupId, int cycleNumber);

    Optional<DrawJpaEntity> findByCycle_IdAndDeletedFalse(UUID cycleId);
}
