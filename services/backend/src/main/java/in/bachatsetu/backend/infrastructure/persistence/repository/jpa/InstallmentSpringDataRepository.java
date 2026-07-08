package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentSpringDataRepository extends BaseJpaRepository<InstallmentJpaEntity> {

    Optional<InstallmentJpaEntity> findByCycle_IdAndMember_IdAndDeletedFalse(UUID cycleId, UUID memberId);

    List<InstallmentJpaEntity> findAllByStatusNotInAndDueDateBetweenAndDeletedFalse(
            Collection<InstallmentStatus> excludedStatuses, LocalDate from, LocalDate to);

    List<InstallmentJpaEntity> findAllByStatusNotInAndDueDateBeforeAndDeletedFalse(
            Collection<InstallmentStatus> excludedStatuses, LocalDate cutoff);
}
