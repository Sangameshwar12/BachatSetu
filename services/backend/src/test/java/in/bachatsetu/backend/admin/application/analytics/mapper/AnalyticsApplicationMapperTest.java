package in.bachatsetu.backend.admin.application.analytics.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.TenantUserCount;
import in.bachatsetu.backend.admin.domain.analytics.model.UploadTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsApplicationMapperTest {

    private final AnalyticsApplicationMapper mapper = new AnalyticsApplicationMapper();

    @Test
    void mapsOverviewAnalytics() {
        OverviewAnalytics analytics = new OverviewAnalytics(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7);

        OverviewAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.totalStoredFiles()).isEqualTo(7);
    }

    @Test
    void mapsPaymentAnalyticsPreservingTheTrend() {
        PaymentAnalytics analytics = new PaymentAnalytics(
                100_000L, 80_000L, 2, 1, 5_000.0, 0.8, 0.2,
                List.of(new PaymentTrendPoint(LocalDate.parse("2026-07-08"), 3, 15_000L)));

        PaymentAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.totalPaymentVolumePaise()).isEqualTo(100_000L);
        assertThat(result.paymentTrend()).hasSize(1);
    }

    @Test
    void mapsGroupAnalytics() {
        GroupAnalytics analytics = new GroupAnalytics(
                10, 8, 2, 5.5, 250_000.0, List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4)), 0.75);

        GroupAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.averageMembersPerGroup()).isEqualTo(5.5);
        assertThat(result.monthlyNewGroups()).hasSize(1);
    }

    @Test
    void mapsUserAnalyticsIncludingTenantCounts() {
        AggregateId tenantId = AggregateId.newId();
        UserAnalytics analytics = new UserAnalytics(
                10, 8, 2, List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4)),
                List.of(new DistributionEntry("ENGLISH", 3)), List.of(new TenantUserCount(tenantId, 3)));

        UserAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.usersPerTenant()).hasSize(1);
        assertThat(result.usersPerTenant().get(0).tenantId()).isEqualTo(tenantId.value());
    }

    @Test
    void mapsNotificationAnalytics() {
        NotificationAnalytics analytics = new NotificationAnalytics(
                10, 5, List.of(new DistributionEntry("DELIVERED", 5)), List.of(new DistributionEntry("PAYMENT_VERIFIED", 5)));

        NotificationAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.unreadNotifications()).isEqualTo(5);
        assertThat(result.deliveryStatusCounts()).hasSize(1);
    }

    @Test
    void mapsStorageAnalytics() {
        StorageAnalytics analytics = new StorageAnalytics(
                4, 4096, 1024.0, List.of(new DistributionEntry("LOCAL", 4)),
                List.of(new UploadTrendPoint(LocalDate.parse("2026-07-08"), 2)));

        StorageAnalyticsResult result = mapper.toResult(analytics);

        assertThat(result.totalStorageBytes()).isEqualTo(4096);
        assertThat(result.uploadsPerDay()).hasSize(1);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> mapper.toResult((OverviewAnalytics) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((PaymentAnalytics) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((GroupAnalytics) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((UserAnalytics) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((NotificationAnalytics) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((StorageAnalytics) null)).isInstanceOf(NullPointerException.class);
    }
}
