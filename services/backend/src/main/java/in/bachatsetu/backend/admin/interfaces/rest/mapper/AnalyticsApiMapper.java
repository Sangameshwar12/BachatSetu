package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.TenantUserCountResult;
import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.model.UploadTrendPoint;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.DistributionEntryResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.GroupAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.MonthlyMetricResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.NotificationAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.OverviewAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.PaymentAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.PaymentTrendPointResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.StorageAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.TenantUserCountResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.UploadTrendPointResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.UserAnalyticsResponse;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps Analytics application-layer read models to safe REST responses. */
@Component
public class AnalyticsApiMapper {

    public ViewAnalyticsCommand toCommand(AuthenticatedUser currentUser) {
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new ViewAnalyticsCommand(currentUser.userId().toAggregateId());
    }

    public OverviewAnalyticsResponse toResponse(OverviewAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new OverviewAnalyticsResponse(
                result.totalUsers(),
                result.activeUsers(),
                result.inactiveUsers(),
                result.totalTenants(),
                result.totalGroups(),
                result.activeGroups(),
                result.completedGroups(),
                result.totalPayments(),
                result.verifiedPayments(),
                result.failedPayments(),
                result.totalReceipts(),
                result.totalNotifications(),
                result.totalStoredFiles());
    }

    public PaymentAnalyticsResponse toResponse(PaymentAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PaymentAnalyticsResponse(
                result.totalPaymentVolumePaise(),
                result.verifiedPaymentVolumePaise(),
                result.failedPaymentCount(),
                result.pendingPaymentCount(),
                result.averageContributionPaise(),
                result.paymentSuccessRate(),
                result.paymentFailureRate(),
                result.paymentTrend().stream().map(this::toResponse).toList());
    }

    public GroupAnalyticsResponse toResponse(GroupAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new GroupAnalyticsResponse(
                result.totalGroups(),
                result.activeGroups(),
                result.completedGroups(),
                result.averageMembersPerGroup(),
                result.averageContributionAmountPaise(),
                result.monthlyNewGroups().stream().map(this::toResponse).toList(),
                result.drawCompletionRate());
    }

    public UserAnalyticsResponse toResponse(UserAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new UserAnalyticsResponse(
                result.totalUsers(),
                result.activeUsers(),
                result.disabledUsers(),
                result.monthlyRegistrations().stream().map(this::toResponse).toList(),
                result.preferredLanguageDistribution().stream().map(this::toResponse).toList(),
                result.usersPerTenant().stream().map(this::toResponse).toList());
    }

    public NotificationAnalyticsResponse toResponse(NotificationAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new NotificationAnalyticsResponse(
                result.totalNotifications(),
                result.unreadNotifications(),
                result.deliveryStatusCounts().stream().map(this::toResponse).toList(),
                result.notificationTypeDistribution().stream().map(this::toResponse).toList());
    }

    public StorageAnalyticsResponse toResponse(StorageAnalyticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new StorageAnalyticsResponse(
                result.totalFiles(),
                result.totalStorageBytes(),
                result.averageFileSizeBytes(),
                result.storageProviderDistribution().stream().map(this::toResponse).toList(),
                result.uploadsPerDay().stream().map(this::toResponse).toList());
    }

    private DistributionEntryResponse toResponse(DistributionEntry entry) {
        return new DistributionEntryResponse(entry.key(), entry.count());
    }

    private MonthlyMetricResponse toResponse(MonthlyMetric metric) {
        return new MonthlyMetricResponse(metric.month().toString(), metric.count());
    }

    private PaymentTrendPointResponse toResponse(PaymentTrendPoint point) {
        return new PaymentTrendPointResponse(point.date().toString(), point.count(), point.volumePaise());
    }

    private UploadTrendPointResponse toResponse(UploadTrendPoint point) {
        return new UploadTrendPointResponse(point.date().toString(), point.count());
    }

    private TenantUserCountResponse toResponse(TenantUserCountResult tenantUserCount) {
        return new TenantUserCountResponse(tenantUserCount.tenantId().toString(), tenantUserCount.userCount());
    }
}
