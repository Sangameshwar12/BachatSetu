package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.TenantUserCountResult;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.model.UploadTrendPoint;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.GroupAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.NotificationAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.OverviewAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.PaymentAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.StorageAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.UserAnalyticsResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsApiMapperTest {

    private final AnalyticsApiMapper mapper = new AnalyticsApiMapper();

    @Test
    void mapsOverviewAnalytics() {
        OverviewAnalyticsResult result =
                new OverviewAnalyticsResult(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7);

        OverviewAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.totalUsers()).isEqualTo(10);
        assertThat(response.totalStoredFiles()).isEqualTo(7);
    }

    @Test
    void mapsPaymentAnalyticsIncludingTheTrendAsIsoDates() {
        PaymentAnalyticsResult result = new PaymentAnalyticsResult(
                100_000L, 80_000L, 2, 1, 5_000.0, 0.8, 0.2,
                List.of(new PaymentTrendPoint(LocalDate.parse("2026-07-08"), 3, 15_000L)));

        PaymentAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.paymentTrend()).hasSize(1);
        assertThat(response.paymentTrend().get(0).date()).isEqualTo("2026-07-08");
        assertThat(response.paymentTrend().get(0).volumePaise()).isEqualTo(15_000L);
    }

    @Test
    void mapsGroupAnalyticsIncludingMonthlyTrend() {
        GroupAnalyticsResult result = new GroupAnalyticsResult(
                10, 8, 2, 5.5, 250_000.0, List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4)), 0.75);

        GroupAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.monthlyNewGroups()).hasSize(1);
        assertThat(response.monthlyNewGroups().get(0).month()).isEqualTo("2026-07");
    }

    @Test
    void mapsUserAnalyticsIncludingTenantCounts() {
        UUID tenantId = UUID.randomUUID();
        UserAnalyticsResult result = new UserAnalyticsResult(
                10, 8, 2, List.of(new MonthlyMetric(YearMonth.of(2026, 7), 4)),
                List.of(new DistributionEntry("ENGLISH", 3)), List.of(new TenantUserCountResult(tenantId, 3)));

        UserAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.usersPerTenant()).hasSize(1);
        assertThat(response.usersPerTenant().get(0).tenantId()).isEqualTo(tenantId.toString());
    }

    @Test
    void mapsNotificationAnalytics() {
        NotificationAnalyticsResult result = new NotificationAnalyticsResult(
                10, 5, List.of(new DistributionEntry("DELIVERED", 5)),
                List.of(new DistributionEntry("PAYMENT_VERIFIED", 5)));

        NotificationAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.unreadNotifications()).isEqualTo(5);
        assertThat(response.deliveryStatusCounts()).hasSize(1);
    }

    @Test
    void mapsStorageAnalyticsIncludingUploadTrend() {
        StorageAnalyticsResult result = new StorageAnalyticsResult(
                4, 4096, 1024.0, List.of(new DistributionEntry("LOCAL", 4)),
                List.of(new UploadTrendPoint(LocalDate.parse("2026-07-08"), 2)));

        StorageAnalyticsResponse response = mapper.toResponse(result);

        assertThat(response.uploadsPerDay()).hasSize(1);
        assertThat(response.uploadsPerDay().get(0).date()).isEqualTo("2026-07-08");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> mapper.toResponse((OverviewAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResponse((PaymentAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResponse((GroupAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResponse((UserAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResponse((NotificationAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResponse((StorageAnalyticsResult) null))
                .isInstanceOf(NullPointerException.class);
    }
}
