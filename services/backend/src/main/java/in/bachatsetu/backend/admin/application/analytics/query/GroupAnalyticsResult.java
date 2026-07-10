package in.bachatsetu.backend.admin.application.analytics.query;

import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import java.util.List;
import java.util.Objects;

/** Application-layer read model mirroring {@code GroupAnalytics}. */
public record GroupAnalyticsResult(
        long totalGroups,
        long activeGroups,
        long completedGroups,
        double averageMembersPerGroup,
        double averageContributionAmountPaise,
        List<MonthlyMetric> monthlyNewGroups,
        double drawCompletionRate) {

    public GroupAnalyticsResult {
        monthlyNewGroups = List.copyOf(Objects.requireNonNull(monthlyNewGroups, "monthlyNewGroups must not be null"));
    }
}
