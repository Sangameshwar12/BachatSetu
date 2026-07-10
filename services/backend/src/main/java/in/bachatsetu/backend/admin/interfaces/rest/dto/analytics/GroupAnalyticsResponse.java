package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Savings group analytics. */
public record GroupAnalyticsResponse(

        @Schema(description = "Total number of savings groups") long totalGroups,
        @Schema(description = "Number of active savings groups") long activeGroups,
        @Schema(description = "Number of completed savings groups") long completedGroups,
        @Schema(description = "Average number of members per group") double averageMembersPerGroup,
        @Schema(description = "Average configured contribution amount, in paise") double averageContributionAmountPaise,
        @Schema(description = "New groups created per month") List<MonthlyMetricResponse> monthlyNewGroups,
        @Schema(description = "Fraction of draws that completed, between 0 and 1") double drawCompletionRate) {

    public GroupAnalyticsResponse {
        monthlyNewGroups = List.copyOf(Objects.requireNonNull(monthlyNewGroups, "monthlyNewGroups must not be null"));
    }
}
