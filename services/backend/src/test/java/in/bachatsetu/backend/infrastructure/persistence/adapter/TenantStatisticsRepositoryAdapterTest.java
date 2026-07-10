package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantStatisticsRepositoryAdapterTest {

    private static final Instant LAST_ACTIVITY = Instant.parse("2026-07-10T08:00:00Z");

    private final UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
    private final SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
    private final PaymentSpringDataRepository paymentRepository = mock(PaymentSpringDataRepository.class);
    private final StoredFileSpringDataRepository storedFileRepository = mock(StoredFileSpringDataRepository.class);
    private final NotificationSpringDataRepository notificationRepository =
            mock(NotificationSpringDataRepository.class);
    private final TenantStatisticsRepositoryAdapter adapter = new TenantStatisticsRepositoryAdapter(
            userRepository, groupRepository, paymentRepository, storedFileRepository, notificationRepository);

    @Test
    void computesStatisticsForOneTenant() {
        UUID tenantId = UUID.randomUUID();
        AggregateId tenant = new AggregateId(tenantId);
        UserJpaEntity mostRecentUser = mock(UserJpaEntity.class);
        when(mostRecentUser.getUpdatedAt()).thenReturn(LAST_ACTIVITY);
        when(userRepository.findFirstByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(tenantId))
                .thenReturn(Optional.of(mostRecentUser));
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(5L);
        when(groupRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(2L);
        when(paymentRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(10L);
        when(paymentRepository.sumAmountPaiseByTenantIdAndStatus(tenantId, PaymentStatus.VERIFIED))
                .thenReturn(50_000L);
        when(storedFileRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(3L);
        when(storedFileRepository.sumSizeByTenantId(tenantId)).thenReturn(1024L);
        when(notificationRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(8L);

        TenantStatistics statistics = adapter.computeFor(tenant);

        assertThat(statistics.users()).isEqualTo(5);
        assertThat(statistics.groups()).isEqualTo(2);
        assertThat(statistics.payments()).isEqualTo(10);
        assertThat(statistics.revenuePaise()).isEqualTo(50_000);
        assertThat(statistics.storageFiles()).isEqualTo(3);
        assertThat(statistics.storageBytes()).isEqualTo(1024);
        assertThat(statistics.notifications()).isEqualTo(8);
        assertThat(statistics.lastActivityAt()).isEqualTo(LAST_ACTIVITY);
    }

    @Test
    void reportsNoLastActivityWhenTheTenantHasNoUsers() {
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findFirstByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(tenantId))
                .thenReturn(Optional.empty());

        TenantStatistics statistics = adapter.computeFor(new AggregateId(tenantId));

        assertThat(statistics.lastActivityAt()).isNull();
    }
}
