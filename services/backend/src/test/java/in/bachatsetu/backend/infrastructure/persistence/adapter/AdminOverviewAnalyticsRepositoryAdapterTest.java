package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

class AdminOverviewAnalyticsRepositoryAdapterTest {

    @Test
    void computesTheOverviewFromEachModulesOwnRepository() {
        UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
        SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
        PaymentSpringDataRepository paymentRepository = mock(PaymentSpringDataRepository.class);
        ReceiptSpringDataRepository receiptRepository = mock(ReceiptSpringDataRepository.class);
        NotificationSpringDataRepository notificationRepository = mock(NotificationSpringDataRepository.class);
        StoredFileSpringDataRepository storedFileRepository = mock(StoredFileSpringDataRepository.class);
        when(userRepository.countByDeletedFalse()).thenReturn(10L);
        when(userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE)).thenReturn(8L);
        when(userRepository.countDistinctTenantIds()).thenReturn(3L);
        when(groupRepository.countByDeletedFalse()).thenReturn(5L);
        when(groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE)).thenReturn(4L);
        when(groupRepository.countByStatusAndDeletedFalse(GroupStatus.CLOSED)).thenReturn(1L);
        when(paymentRepository.countByDeletedFalse()).thenReturn(20L);
        when(paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED)).thenReturn(15L);
        when(paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.FAILED)).thenReturn(2L);
        when(receiptRepository.countByDeletedFalse()).thenReturn(15L);
        when(notificationRepository.countByDeletedFalse()).thenReturn(30L);
        when(storedFileRepository.countByDeletedFalse()).thenReturn(7L);
        AdminOverviewAnalyticsRepositoryAdapter adapter = new AdminOverviewAnalyticsRepositoryAdapter(
                userRepository, groupRepository, paymentRepository, receiptRepository, notificationRepository,
                storedFileRepository);

        OverviewAnalytics analytics = adapter.compute();

        assertThat(analytics).isEqualTo(new OverviewAnalytics(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7));
    }
}
