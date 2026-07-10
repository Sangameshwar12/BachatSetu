package in.bachatsetu.backend.admin.application.analytics.query;

/** Application-layer read model mirroring {@code OverviewAnalytics}. */
public record OverviewAnalyticsResult(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long totalTenants,
        long totalGroups,
        long activeGroups,
        long completedGroups,
        long totalPayments,
        long verifiedPayments,
        long failedPayments,
        long totalReceipts,
        long totalNotifications,
        long totalStoredFiles) {
}
