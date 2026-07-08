package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.domain.model.DueInstallment;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.InstallmentSpringDataRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstallmentReminderRepositoryAdapterTest {

    private InstallmentSpringDataRepository repository;
    private InstallmentReminderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(InstallmentSpringDataRepository.class);
        adapter = new InstallmentReminderRepositoryAdapter(repository);
    }

    @Test
    void findsInstallmentsDueBetweenAndComputesTheOutstandingAmount() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InstallmentJpaEntity entity = newInstallment(tenantId, userId, "Sunrise Bhishi", 50_000L, 20_000L);
        LocalDate from = LocalDate.of(2026, 7, 8);
        LocalDate to = LocalDate.of(2026, 7, 11);
        when(repository.findAllByStatusNotInAndDueDateBetweenAndDeletedFalse(any(), eq(from), eq(to)))
                .thenReturn(List.of(entity));

        List<DueInstallment> due = adapter.findDueBetween(from, to);

        assertThat(due).hasSize(1);
        DueInstallment result = due.getFirst();
        assertThat(result.tenantId().value()).isEqualTo(tenantId);
        assertThat(result.recipientUserId().value()).isEqualTo(userId);
        assertThat(result.groupName()).isEqualTo("Sunrise Bhishi");
        assertThat(result.outstandingAmountPaise()).isEqualTo(30_000L);
        assertThat(result.currencyCode()).isEqualTo("INR");
    }

    @Test
    void clampsTheOutstandingAmountAtZeroWhenOverpaid() {
        InstallmentJpaEntity entity = newInstallment(UUID.randomUUID(), UUID.randomUUID(), "Group", 10_000L, 15_000L);
        LocalDate cutoff = LocalDate.of(2026, 7, 8);
        when(repository.findAllByStatusNotInAndDueDateBeforeAndDeletedFalse(any(), eq(cutoff)))
                .thenReturn(List.of(entity));

        List<DueInstallment> overdue = adapter.findOverdueBefore(cutoff);

        assertThat(overdue.getFirst().outstandingAmountPaise()).isZero();
    }

    @Test
    void returnsAnEmptyListWhenNothingMatches() {
        LocalDate cutoff = LocalDate.of(2026, 7, 8);
        when(repository.findAllByStatusNotInAndDueDateBeforeAndDeletedFalse(any(), eq(cutoff)))
                .thenReturn(List.of());

        assertThat(adapter.findOverdueBefore(cutoff)).isEmpty();
    }

    private InstallmentJpaEntity newInstallment(
            UUID tenantId, UUID userId, String groupName, long expectedPaise, long paidPaise) {
        InstallmentJpaEntity entity = mock(InstallmentJpaEntity.class);
        SavingsGroupJpaEntity group = mock(SavingsGroupJpaEntity.class);
        GroupMemberJpaEntity member = mock(GroupMemberJpaEntity.class);
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getTenantId()).thenReturn(tenantId);
        when(entity.getGroup()).thenReturn(group);
        when(group.getName()).thenReturn(groupName);
        when(entity.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(entity.getExpectedAmountPaise()).thenReturn(expectedPaise);
        when(entity.getPaidAmountPaise()).thenReturn(paidPaise);
        when(entity.getCurrencyCode()).thenReturn("INR");
        when(entity.getDueDate()).thenReturn(LocalDate.of(2026, 7, 10));
        return entity;
    }
}
