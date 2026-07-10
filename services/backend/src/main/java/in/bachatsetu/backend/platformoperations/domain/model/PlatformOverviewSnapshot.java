package in.bachatsetu.backend.platformoperations.domain.model;

/** Platform-wide snapshot for the Super Admin Dashboard, computed on demand — no scheduled aggregation. */
public record PlatformOverviewSnapshot(
        long totalUsers,
        long totalOrganizers,
        long totalGroups,
        long totalMembers,
        long totalPayments,
        long totalReceipts,
        long totalNotifications,
        long totalStoredFiles,
        long totalActiveTenants,
        long totalRevenuePaise,
        long todaySignups,
        long todayPayments,
        long todayGroups,
        long todayNotifications,
        long todayStorageUploads) {
}
