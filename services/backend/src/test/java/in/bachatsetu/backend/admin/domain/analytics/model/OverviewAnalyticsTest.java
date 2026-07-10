package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OverviewAnalyticsTest {

    @Test
    void recordsEveryField() {
        OverviewAnalytics analytics = new OverviewAnalytics(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7);

        assertThat(analytics.totalUsers()).isEqualTo(10);
        assertThat(analytics.activeUsers()).isEqualTo(8);
        assertThat(analytics.inactiveUsers()).isEqualTo(2);
        assertThat(analytics.totalTenants()).isEqualTo(3);
        assertThat(analytics.totalStoredFiles()).isEqualTo(7);
    }

    @Test
    void rejectsANegativeField() {
        assertThatThrownBy(() -> new OverviewAnalytics(-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
