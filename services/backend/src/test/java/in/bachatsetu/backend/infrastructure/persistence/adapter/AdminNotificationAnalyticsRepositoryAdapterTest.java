package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminNotificationAnalyticsRepositoryAdapterTest {

    @Test
    void computesUnreadCountAsNotYetDelivered() {
        NotificationSpringDataRepository repository = mock(NotificationSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(10L);
        when(repository.findStatusDistribution()).thenReturn(List.of(
                new Object[] {NotificationStatus.DELIVERED, 6L},
                new Object[] {NotificationStatus.FAILED, 2L},
                new Object[] {NotificationStatus.QUEUED, 2L}));
        when(repository.findCategoryDistribution())
                .thenReturn(List.<Object[]>of(new Object[] {NotificationCategory.PAYMENT, 10L}));
        AdminNotificationAnalyticsRepositoryAdapter adapter =
                new AdminNotificationAnalyticsRepositoryAdapter(repository);

        NotificationAnalytics analytics = adapter.compute();

        assertThat(analytics.totalNotifications()).isEqualTo(10L);
        assertThat(analytics.unreadNotifications()).isEqualTo(4L);
        assertThat(analytics.deliveryStatusCounts()).hasSize(3);
        assertThat(analytics.notificationTypeDistribution()).hasSize(1);
    }

    @Test
    void treatsEveryNotificationAsUnreadWhenNoneAreDelivered() {
        NotificationSpringDataRepository repository = mock(NotificationSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(3L);
        when(repository.findStatusDistribution())
                .thenReturn(List.<Object[]>of(new Object[] {NotificationStatus.QUEUED, 3L}));
        when(repository.findCategoryDistribution()).thenReturn(List.of());
        AdminNotificationAnalyticsRepositoryAdapter adapter =
                new AdminNotificationAnalyticsRepositoryAdapter(repository);

        assertThat(adapter.compute().unreadNotifications()).isEqualTo(3L);
    }
}
