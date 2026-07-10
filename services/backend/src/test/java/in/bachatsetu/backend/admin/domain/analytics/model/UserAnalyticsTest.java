package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalyticsTest {

    @Test
    void recordsEveryField() {
        List<MonthlyMetric> monthly = List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4));
        List<DistributionEntry> languages = List.of(new DistributionEntry("ENGLISH", 3));
        List<TenantUserCount> perTenant = List.of(new TenantUserCount(AggregateId.newId(), 3));

        UserAnalytics analytics = new UserAnalytics(10, 8, 2, monthly, languages, perTenant);

        assertThat(analytics.totalUsers()).isEqualTo(10);
        assertThat(analytics.monthlyRegistrations()).hasSize(1);
        assertThat(analytics.preferredLanguageDistribution()).hasSize(1);
        assertThat(analytics.usersPerTenant()).hasSize(1);
    }

    @Test
    void rejectsANullLanguageDistribution() {
        assertThatThrownBy(() -> new UserAnalytics(0, 0, 0, List.of(), null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeDisabledUsers() {
        assertThatThrownBy(() -> new UserAnalytics(0, 0, -1, List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
