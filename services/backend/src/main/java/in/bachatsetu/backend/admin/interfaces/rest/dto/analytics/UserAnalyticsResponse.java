package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Platform user analytics. */
public record UserAnalyticsResponse(

        @Schema(description = "Total number of users") long totalUsers,
        @Schema(description = "Number of active users") long activeUsers,
        @Schema(description = "Number of disabled users") long disabledUsers,
        @Schema(description = "New registrations per month") List<MonthlyMetricResponse> monthlyRegistrations,
        @Schema(description = "Preferred language distribution")
        List<DistributionEntryResponse> preferredLanguageDistribution,
        @Schema(description = "Users per tenant") List<TenantUserCountResponse> usersPerTenant) {

    public UserAnalyticsResponse {
        monthlyRegistrations =
                List.copyOf(Objects.requireNonNull(monthlyRegistrations, "monthlyRegistrations must not be null"));
        preferredLanguageDistribution = List.copyOf(Objects.requireNonNull(
                preferredLanguageDistribution, "preferredLanguageDistribution must not be null"));
        usersPerTenant = List.copyOf(Objects.requireNonNull(usersPerTenant, "usersPerTenant must not be null"));
    }
}
