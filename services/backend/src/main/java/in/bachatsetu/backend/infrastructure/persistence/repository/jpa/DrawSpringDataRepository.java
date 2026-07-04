package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DrawSpringDataRepository extends BaseJpaRepository<DrawJpaEntity> {

    Optional<DrawJpaEntity> findByGroup_IdAndCycle_CycleNumberAndDeletedFalse(
            UUID groupId, int cycleNumber);

    Optional<DrawJpaEntity> findByCycle_IdAndDeletedFalse(UUID cycleId);
}
