package in.bachatsetu.backend.platformoperations.application.query;

public record PlatformOverviewResult(
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
