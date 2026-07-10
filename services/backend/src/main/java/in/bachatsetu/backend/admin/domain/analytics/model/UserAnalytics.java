package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/** Platform user analytics. */
public record UserAnalytics(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        List<MonthlyMetric> monthlyRegistrations,
        List<DistributionEntry> preferredLanguageDistribution,
        List<TenantUserCount> usersPerTenant) {

    public UserAnalytics {
        if (totalUsers < 0) {
            throw new IllegalArgumentException("totalUsers must not be negative");
        }
        if (activeUsers < 0) {
            throw new IllegalArgumentException("activeUsers must not be negative");
        }
        if (disabledUsers < 0) {
            throw new IllegalArgumentException("disabledUsers must not be negative");
        }
        monthlyRegistrations =
                List.copyOf(Objects.requireNonNull(monthlyRegistrations, "monthlyRegistrations must not be null"));
        preferredLanguageDistribution = List.copyOf(Objects.requireNonNull(
                preferredLanguageDistribution, "preferredLanguageDistribution must not be null"));
        usersPerTenant = List.copyOf(Objects.requireNonNull(usersPerTenant, "usersPerTenant must not be null"));
    }
}
