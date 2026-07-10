package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ValueTypesTest {

    @Test
    void distributionEntryRejectsANullKey() {
        assertThatThrownBy(() -> new DistributionEntry(null, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void distributionEntryRejectsANegativeCount() {
        assertThatThrownBy(() -> new DistributionEntry("key", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void monthlyMetricRejectsANullMonth() {
        assertThatThrownBy(() -> new MonthlyMetric(null, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void monthlyMetricRejectsANegativeCount() {
        assertThatThrownBy(() -> new MonthlyMetric(YearMonth.of(2026, 7), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paymentTrendPointRejectsANullDate() {
        assertThatThrownBy(() -> new PaymentTrendPoint(null, 0, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void paymentTrendPointRejectsANegativeVolume() {
        assertThatThrownBy(() -> new PaymentTrendPoint(LocalDate.parse("2026-07-08"), 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadTrendPointRejectsANegativeCount() {
        assertThatThrownBy(() -> new UploadTrendPoint(LocalDate.parse("2026-07-08"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tenantUserCountRejectsANullTenantId() {
        assertThatThrownBy(() -> new TenantUserCount(null, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void tenantUserCountRejectsANegativeUserCount() {
        assertThatThrownBy(() -> new TenantUserCount(AggregateId.newId(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
