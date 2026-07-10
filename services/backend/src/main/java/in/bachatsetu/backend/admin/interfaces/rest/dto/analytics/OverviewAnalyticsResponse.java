package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** Platform-wide overview snapshot. */
public record OverviewAnalyticsResponse(

        @Schema(description = "Total number of users") long totalUsers,
        @Schema(description = "Number of active users") long activeUsers,
        @Schema(description = "Number of inactive (not active) users") long inactiveUsers,
        @Schema(description = "Total number of tenants") long totalTenants,
        @Schema(description = "Total number of savings groups") long totalGroups,
        @Schema(description = "Number of active savings groups") long activeGroups,
        @Schema(description = "Number of completed savings groups") long completedGroups,
        @Schema(description = "Total number of payments") long totalPayments,
        @Schema(description = "Number of verified payments") long verifiedPayments,
        @Schema(description = "Number of failed payments") long failedPayments,
        @Schema(description = "Total number of receipts") long totalReceipts,
        @Schema(description = "Total number of notifications") long totalNotifications,
        @Schema(description = "Total number of stored files") long totalStoredFiles) {
}
