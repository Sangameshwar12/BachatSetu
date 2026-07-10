package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.TenantSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.platformoperations.domain.model.PlatformOverviewSnapshot;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformOverviewRepositoryAdapterTest {

    private static final Instant TODAY_START = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant TODAY_END = Instant.parse("2026-07-11T00:00:00Z");

    private final UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
    private final SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
    private final MemberSpringDataRepository memberRepository = mock(MemberSpringDataRepository.class);
    private final PaymentSpringDataRepository paymentRepository = mock(PaymentSpringDataRepository.class);
    private final ReceiptSpringDataRepository receiptRepository = mock(ReceiptSpringDataRepository.class);
    private final NotificationSpringDataRepository notificationRepository =
            mock(NotificationSpringDataRepository.class);
    private final StoredFileSpringDataRepository storedFileRepository = mock(StoredFileSpringDataRepository.class);
    private final TenantSpringDataRepository tenantRepository = mock(TenantSpringDataRepository.class);
    private final PlatformOverviewRepositoryAdapter adapter = new PlatformOverviewRepositoryAdapter(
            userRepository, groupRepository, memberRepository, paymentRepository, receiptRepository,
            notificationRepository, storedFileRepository, tenantRepository);

    @Test
    void computesActiveTenantsAsKnownTenantsMinusSuspendedAndArchived() {
        when(userRepository.countByDeletedFalse()).thenReturn(100L);
        when(memberRepository.countDistinctUsersByRoleAndDeletedFalse(GroupRole.ORGANIZER)).thenReturn(20L);
        when(groupRepository.countByDeletedFalse()).thenReturn(30L);
        when(memberRepository.countByDeletedFalse()).thenReturn(150L);
        when(paymentRepository.countByDeletedFalse()).thenReturn(500L);
        when(receiptRepository.countByDeletedFalse()).thenReturn(400L);
        when(notificationRepository.countByDeletedFalse()).thenReturn(600L);
        when(storedFileRepository.countByDeletedFalse()).thenReturn(50L);
        when(userRepository.countDistinctTenantIds()).thenReturn(10L);
        when(tenantRepository.countByStatusAndDeletedFalse(TenantStatus.SUSPENDED)).thenReturn(2L);
        when(tenantRepository.countByStatusAndDeletedFalse(TenantStatus.ARCHIVED)).thenReturn(1L);
        when(paymentRepository.sumAmountPaiseByStatus(PaymentStatus.VERIFIED)).thenReturn(1_000_000L);
        when(userRepository.countByCreatedAtBetween(TODAY_START, TODAY_END)).thenReturn(5L);
        when(paymentRepository.countByCreatedAtBetween(TODAY_START, TODAY_END)).thenReturn(6L);
        when(groupRepository.countByCreatedAtBetween(TODAY_START, TODAY_END)).thenReturn(7L);
        when(notificationRepository.countByCreatedAtBetween(TODAY_START, TODAY_END)).thenReturn(8L);
        when(storedFileRepository.countByUploadedAtBetween(TODAY_START, TODAY_END)).thenReturn(9L);

        PlatformOverviewSnapshot snapshot = adapter.compute(TODAY_START, TODAY_END);

        assertThat(snapshot.totalActiveTenants()).isEqualTo(7);
        assertThat(snapshot.totalUsers()).isEqualTo(100);
        assertThat(snapshot.totalOrganizers()).isEqualTo(20);
        assertThat(snapshot.totalRevenuePaise()).isEqualTo(1_000_000);
        assertThat(snapshot.todaySignups()).isEqualTo(5);
        assertThat(snapshot.todayStorageUploads()).isEqualTo(9);
    }

    @Test
    void neverReturnsNegativeActiveTenants() {
        when(userRepository.countDistinctTenantIds()).thenReturn(1L);
        when(tenantRepository.countByStatusAndDeletedFalse(TenantStatus.SUSPENDED)).thenReturn(1L);
        when(tenantRepository.countByStatusAndDeletedFalse(TenantStatus.ARCHIVED)).thenReturn(1L);

        PlatformOverviewSnapshot snapshot = adapter.compute(TODAY_START, TODAY_END);

        assertThat(snapshot.totalActiveTenants()).isEqualTo(0);
    }
}
