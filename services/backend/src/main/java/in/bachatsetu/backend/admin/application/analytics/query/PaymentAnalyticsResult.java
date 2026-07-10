package in.bachatsetu.backend.admin.application.analytics.query;

import in.bachatsetu.backend.admin.domain.analytics.model.PaymentTrendPoint;
import java.util.List;
import java.util.Objects;

/** Application-layer read model mirroring {@code PaymentAnalytics}. */
public record PaymentAnalyticsResult(
        long totalPaymentVolumePaise,
        long verifiedPaymentVolumePaise,
        long failedPaymentCount,
        long pendingPaymentCount,
        double averageContributionPaise,
        double paymentSuccessRate,
        double paymentFailureRate,
        List<PaymentTrendPoint> paymentTrend) {

    public PaymentAnalyticsResult {
        paymentTrend = List.copyOf(Objects.requireNonNull(paymentTrend, "paymentTrend must not be null"));
    }
}
