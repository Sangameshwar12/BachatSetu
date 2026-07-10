package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
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

class AdminStatisticsRepositoryAdapterTest {

    @Test
    void computesEveryTotalFromEachModulesOwnRepository() {
        UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
        SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
        PaymentSpringDataRepository paymentRepository = mock(PaymentSpringDataRepository.class);
        ReceiptSpringDataRepository receiptRepository = mock(ReceiptSpringDataRepository.class);
        NotificationSpringDataRepository notificationRepository = mock(NotificationSpringDataRepository.class);
        StoredFileSpringDataRepository storedFileRepository = mock(StoredFileSpringDataRepository.class);
        when(userRepository.countByDeletedFalse()).thenReturn(10L);
        when(userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE)).thenReturn(8L);
        when(userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.DISABLED)).thenReturn(2L);
        when(groupRepository.countByDeletedFalse()).thenReturn(5L);
        when(groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE)).thenReturn(4L);
        when(paymentRepository.countByDeletedFalse()).thenReturn(20L);
        when(paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED)).thenReturn(15L);
        when(receiptRepository.countByDeletedFalse()).thenReturn(15L);
        when(notificationRepository.countByDeletedFalse()).thenReturn(30L);
        when(storedFileRepository.countByDeletedFalse()).thenReturn(7L);
        AdminStatisticsRepositoryAdapter adapter = new AdminStatisticsRepositoryAdapter(
                userRepository, groupRepository, paymentRepository, receiptRepository, notificationRepository,
                storedFileRepository);

        PlatformStatistics statistics = adapter.compute();

        assertThat(statistics).isEqualTo(new PlatformStatistics(10, 8, 2, 5, 4, 20, 15, 15, 30, 7));
    }
}
