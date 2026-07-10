package in.bachatsetu.backend.admin.domain.model;

/** Platform-wide totals, computed on demand through existing repositories — never cached or materialized. */
public record PlatformStatistics(
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

    public PlatformStatistics {
        requireNonNegative(totalUsers, "totalUsers");
        requireNonNegative(activeUsers, "activeUsers");
        requireNonNegative(disabledUsers, "disabledUsers");
        requireNonNegative(totalGroups, "totalGroups");
        requireNonNegative(activeGroups, "activeGroups");
        requireNonNegative(totalPayments, "totalPayments");
        requireNonNegative(completedPayments, "completedPayments");
        requireNonNegative(totalReceipts, "totalReceipts");
        requireNonNegative(totalNotifications, "totalNotifications");
        requireNonNegative(totalFiles, "totalFiles");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
