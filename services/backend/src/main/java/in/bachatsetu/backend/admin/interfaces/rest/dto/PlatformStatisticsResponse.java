package in.bachatsetu.backend.admin.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe presentation view of platform-wide totals returned by the REST API. */
public record PlatformStatisticsResponse(

        @Schema(description = "Total number of users") long totalUsers,
        @Schema(description = "Number of active users") long activeUsers,
        @Schema(description = "Number of disabled users") long disabledUsers,
        @Schema(description = "Total number of savings groups") long totalGroups,
        @Schema(description = "Number of active savings groups") long activeGroups,
        @Schema(description = "Total number of payments") long totalPayments,
        @Schema(description = "Number of completed (verified) payments") long completedPayments,
        @Schema(description = "Total number of receipts") long totalReceipts,
        @Schema(description = "Total number of notifications") long totalNotifications,
        @Schema(description = "Total number of stored files") long totalFiles) {
}
