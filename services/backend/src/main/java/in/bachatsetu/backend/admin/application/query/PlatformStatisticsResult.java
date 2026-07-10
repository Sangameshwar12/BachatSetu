package in.bachatsetu.backend.admin.application.query;

/** Application-layer read model for platform-wide totals. */
public record PlatformStatisticsResult(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long totalGroups,
        long activeGroups,
        long totalPayments,
        long completedPayments,
        long totalReceipts,
        long totalNotifications,
        long totalFiles) {
}
