package in.bachatsetu.backend.dashboard.application.query;

import java.util.List;
import java.util.Objects;

/** Composed, real-time view for the Welcome/Member dashboard: no field is placeholder data. */
public record MemberDashboardResult(
        CurrentGroupSummary currentGroup,
        NextDrawSummary nextDraw,
        String latestPaymentStatus,
        List<RecentNotificationSummary> recentNotifications) {

    public MemberDashboardResult {
        recentNotifications = List.copyOf(
                Objects.requireNonNull(recentNotifications, "recentNotifications must not be null"));
    }
}
