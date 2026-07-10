package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroupAnalyticsTest {

    @Test
    void recordsEveryField() {
        List<MonthlyMetric> monthly = List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4));

        GroupAnalytics analytics = new GroupAnalytics(10, 8, 2, 5.5, 250_000.0, monthly, 0.75);

        assertThat(analytics.totalGroups()).isEqualTo(10);
        assertThat(analytics.averageMembersPerGroup()).isEqualTo(5.5);
        assertThat(analytics.monthlyNewGroups()).hasSize(1);
        assertThat(analytics.drawCompletionRate()).isEqualTo(0.75);
    }

    @Test
    void rejectsADrawCompletionRateAboveOne() {
        assertThatThrownBy(() -> new GroupAnalytics(0, 0, 0, 0.0, 0.0, List.of(), 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullMonthlyList() {
        assertThatThrownBy(() -> new GroupAnalytics(0, 0, 0, 0.0, 0.0, null, 0.0))
                .isInstanceOf(NullPointerException.class);
    }
}
