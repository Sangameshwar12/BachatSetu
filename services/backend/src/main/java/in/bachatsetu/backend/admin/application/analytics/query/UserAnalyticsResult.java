package in.bachatsetu.backend.admin.application.analytics.query;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import java.util.List;
import java.util.Objects;

/** Application-layer read model mirroring {@code UserAnalytics}. */
public record UserAnalyticsResult(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        List<MonthlyMetric> monthlyRegistrations,
        List<DistributionEntry> preferredLanguageDistribution,
        List<TenantUserCountResult> usersPerTenant) {

    public UserAnalyticsResult {
        monthlyRegistrations =
                List.copyOf(Objects.requireNonNull(monthlyRegistrations, "monthlyRegistrations must not be null"));
        preferredLanguageDistribution = List.copyOf(Objects.requireNonNull(
                preferredLanguageDistribution, "preferredLanguageDistribution must not be null"));
        usersPerTenant = List.copyOf(Objects.requireNonNull(usersPerTenant, "usersPerTenant must not be null"));
    }
}
