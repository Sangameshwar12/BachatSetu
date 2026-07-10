package in.bachatsetu.backend.admin.domain.analytics.model;

/** Platform-wide snapshot across every existing module, computed fresh on every request. */
public record OverviewAnalytics(
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

    public OverviewAnalytics {
        requireNonNegative(totalUsers, "totalUsers");
        requireNonNegative(activeUsers, "activeUsers");
        requireNonNegative(inactiveUsers, "inactiveUsers");
        requireNonNegative(totalTenants, "totalTenants");
        requireNonNegative(totalGroups, "totalGroups");
        requireNonNegative(activeGroups, "activeGroups");
        requireNonNegative(completedGroups, "completedGroups");
        requireNonNegative(totalPayments, "totalPayments");
        requireNonNegative(verifiedPayments, "verifiedPayments");
        requireNonNegative(failedPayments, "failedPayments");
        requireNonNegative(totalReceipts, "totalReceipts");
        requireNonNegative(totalNotifications, "totalNotifications");
        requireNonNegative(totalStoredFiles, "totalStoredFiles");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
