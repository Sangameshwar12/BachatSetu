package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import in.bachatsetu.backend.automation.domain.port.InstallmentReminderRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.InstallmentSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persistence adapter over the pre-existing {@code community.installments} table. */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class InstallmentReminderRepositoryAdapter implements InstallmentReminderRepository {

    private static final List<InstallmentStatus> EXCLUDED_STATUSES = List.of(
            InstallmentStatus.PAID, InstallmentStatus.WAIVED,
            InstallmentStatus.CANCELLED, InstallmentStatus.DISPUTED);

    private final InstallmentSpringDataRepository repository;

    public InstallmentReminderRepositoryAdapter(InstallmentSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<DueInstallment> findDueBetween(LocalDate from, LocalDate to) {
        return repository.findAllByStatusNotInAndDueDateBetweenAndDeletedFalse(EXCLUDED_STATUSES, from, to)
                .stream().map(this::toDueInstallment).toList();
    }

    @Override
    public List<DueInstallment> findOverdueBefore(LocalDate cutoff) {
        return repository.findAllByStatusNotInAndDueDateBeforeAndDeletedFalse(EXCLUDED_STATUSES, cutoff)
                .stream().map(this::toDueInstallment).toList();
    }

    private DueInstallment toDueInstallment(InstallmentJpaEntity entity) {
        long outstanding = Math.max(0, entity.getExpectedAmountPaise() - entity.getPaidAmountPaise());
        return new DueInstallment(
                new AggregateId(entity.getTenantId()),
                new AggregateId(entity.getId()),
                new AggregateId(entity.getMember().getUser().getId()),
                entity.getGroup().getName(),
                outstanding,
                entity.getCurrencyCode(),
                entity.getDueDate());
    }
}
