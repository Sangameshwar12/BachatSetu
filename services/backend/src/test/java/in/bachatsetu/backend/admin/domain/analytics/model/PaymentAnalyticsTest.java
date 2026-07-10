package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentAnalyticsTest {

    private static final LocalDate DAY = LocalDate.parse("2026-07-08");

    @Test
    void recordsEveryFieldIncludingTheTrend() {
        List<PaymentTrendPoint> trend = List.of(new PaymentTrendPoint(DAY, 3, 15_000L));

        PaymentAnalytics analytics = new PaymentAnalytics(100_000L, 80_000L, 2, 1, 5_000.0, 0.8, 0.2, trend);

        assertThat(analytics.totalPaymentVolumePaise()).isEqualTo(100_000L);
        assertThat(analytics.paymentTrend()).hasSize(1);
        assertThat(analytics.paymentTrend().get(0).count()).isEqualTo(3);
    }

    @Test
    void rejectsASuccessRateAboveOne() {
        assertThatThrownBy(() -> new PaymentAnalytics(0, 0, 0, 0, 0.0, 1.1, 0.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAFailureRateBelowZero() {
        assertThatThrownBy(() -> new PaymentAnalytics(0, 0, 0, 0, 0.0, 0.0, -0.1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullTrend() {
        assertThatThrownBy(() -> new PaymentAnalytics(0, 0, 0, 0, 0.0, 0.0, 0.0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeVolume() {
        assertThatThrownBy(() -> new PaymentAnalytics(-1, 0, 0, 0, 0.0, 0.0, 0.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
