package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/** Savings group analytics. {@code drawCompletionRate} is a fraction in {@code [0, 1]}. */
public record GroupAnalytics(
        long totalGroups,
        long activeGroups,
        long completedGroups,
        double averageMembersPerGroup,
        double averageContributionAmountPaise,
        List<MonthlyMetric> monthlyNewGroups,
        double drawCompletionRate) {

    public GroupAnalytics {
        if (totalGroups < 0) {
            throw new IllegalArgumentException("totalGroups must not be negative");
        }
        if (activeGroups < 0) {
            throw new IllegalArgumentException("activeGroups must not be negative");
        }
        if (completedGroups < 0) {
            throw new IllegalArgumentException("completedGroups must not be negative");
        }
        if (drawCompletionRate < 0 || drawCompletionRate > 1) {
            throw new IllegalArgumentException("drawCompletionRate must be between 0 and 1");
        }
        monthlyNewGroups = List.copyOf(Objects.requireNonNull(monthlyNewGroups, "monthlyNewGroups must not be null"));
    }
}
