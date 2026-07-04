package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentSpringDataRepository extends BaseJpaRepository<InstallmentJpaEntity> {

    Optional<InstallmentJpaEntity> findByCycle_IdAndMember_IdAndDeletedFalse(UUID cycleId, UUID memberId);
}
