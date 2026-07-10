package in.bachatsetu.backend.admin.application.analytics.mapper;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.TenantUserCountResult;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.TenantUserCount;
import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import java.util.Objects;

/** Converts Analytics domain snapshots to application-layer read models. */
public final class AnalyticsApplicationMapper {

    public OverviewAnalyticsResult toResult(OverviewAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new OverviewAnalyticsResult(
                analytics.totalUsers(),
                analytics.activeUsers(),
                analytics.inactiveUsers(),
                analytics.totalTenants(),
                analytics.totalGroups(),
                analytics.activeGroups(),
                analytics.completedGroups(),
                analytics.totalPayments(),
                analytics.verifiedPayments(),
                analytics.failedPayments(),
                analytics.totalReceipts(),
                analytics.totalNotifications(),
                analytics.totalStoredFiles());
    }

    public PaymentAnalyticsResult toResult(PaymentAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new PaymentAnalyticsResult(
                analytics.totalPaymentVolumePaise(),
                analytics.verifiedPaymentVolumePaise(),
                analytics.failedPaymentCount(),
                analytics.pendingPaymentCount(),
                analytics.averageContributionPaise(),
                analytics.paymentSuccessRate(),
                analytics.paymentFailureRate(),
                analytics.paymentTrend());
    }

    public GroupAnalyticsResult toResult(GroupAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new GroupAnalyticsResult(
                analytics.totalGroups(),
                analytics.activeGroups(),
                analytics.completedGroups(),
                analytics.averageMembersPerGroup(),
                analytics.averageContributionAmountPaise(),
                analytics.monthlyNewGroups(),
                analytics.drawCompletionRate());
    }

    public UserAnalyticsResult toResult(UserAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new UserAnalyticsResult(
                analytics.totalUsers(),
                analytics.activeUsers(),
                analytics.disabledUsers(),
                analytics.monthlyRegistrations(),
                analytics.preferredLanguageDistribution(),
                analytics.usersPerTenant().stream().map(this::toResult).toList());
    }

    public NotificationAnalyticsResult toResult(NotificationAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new NotificationAnalyticsResult(
                analytics.totalNotifications(),
                analytics.unreadNotifications(),
                analytics.deliveryStatusCounts(),
                analytics.notificationTypeDistribution());
    }

    public StorageAnalyticsResult toResult(StorageAnalytics analytics) {
        Objects.requireNonNull(analytics, "analytics must not be null");
        return new StorageAnalyticsResult(
                analytics.totalFiles(),
                analytics.totalStorageBytes(),
                analytics.averageFileSizeBytes(),
                analytics.storageProviderDistribution(),
                analytics.uploadsPerDay());
    }

    private TenantUserCountResult toResult(TenantUserCount tenantUserCount) {
        return new TenantUserCountResult(tenantUserCount.tenantId().value(), tenantUserCount.userCount());
    }
}
