package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyCycleSpringDataRepository extends BaseJpaRepository<MonthlyCycleJpaEntity> {

    Optional<MonthlyCycleJpaEntity> findByGroup_IdAndCycleNumberAndDeletedFalse(UUID groupId, int cycleNumber);
}
