package in.bachatsetu.backend.dashboard.application.query;

import java.util.List;
import java.util.Objects;

/** Composed, real-time view of every group an organizer owns, plus available quick actions. */
public record OrganizerDashboardResult(List<OrganizerGroupSummary> groups, List<QuickAction> quickActions) {

    public OrganizerDashboardResult {
        groups = List.copyOf(Objects.requireNonNull(groups, "groups must not be null"));
        quickActions = List.copyOf(Objects.requireNonNull(quickActions, "quickActions must not be null"));
    }
}
