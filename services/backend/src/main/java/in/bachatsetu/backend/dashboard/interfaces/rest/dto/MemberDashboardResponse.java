package in.bachatsetu.backend.dashboard.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record MemberDashboardResponse(
        CurrentGroupResponse currentGroup,
        NextDrawResponse nextDraw,
        @Schema(description = "The caller's most recent payment status toward this group, if any")
        String latestPaymentStatus,
        List<NotificationSummaryResponse> recentNotifications) {

    public MemberDashboardResponse {
        recentNotifications = List.copyOf(recentNotifications);
    }

    public record CurrentGroupResponse(
            String groupId,
            String groupCode,
            String name,
            long upcomingInstallmentAmountPaise,
            String currencyCode,
            String frequency,
            int memberCount,
            int maximumMembers) {
    }

    public record NextDrawResponse(String drawId, Instant scheduledAt, String status) {
    }

    public record NotificationSummaryResponse(
            String notificationId, String category, String status, Instant createdAt) {
    }
}
