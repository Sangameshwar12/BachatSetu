package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationAnalyticsTest {

    @Test
    void recordsEveryField() {
        List<DistributionEntry> statuses = List.of(new DistributionEntry("DELIVERED", 5));
        List<DistributionEntry> categories = List.of(new DistributionEntry("PAYMENT_VERIFIED", 5));

        NotificationAnalytics analytics = new NotificationAnalytics(10, 5, statuses, categories);

        assertThat(analytics.totalNotifications()).isEqualTo(10);
        assertThat(analytics.unreadNotifications()).isEqualTo(5);
        assertThat(analytics.deliveryStatusCounts()).hasSize(1);
    }

    @Test
    void rejectsANullDistribution() {
        assertThatThrownBy(() -> new NotificationAnalytics(0, 0, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeUnreadCount() {
        assertThatThrownBy(() -> new NotificationAnalytics(0, -1, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
