package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/**
 * Payment-focused analytics. {@code paymentSuccessRate}/{@code paymentFailureRate} are fractions in
 * {@code [0, 1]}, both {@code 0} when there are no payments at all (never divides by zero).
 */
public record PaymentAnalytics(
        long totalPaymentVolumePaise,
        long verifiedPaymentVolumePaise,
        long failedPaymentCount,
        long pendingPaymentCount,
        double averageContributionPaise,
        double paymentSuccessRate,
        double paymentFailureRate,
        List<PaymentTrendPoint> paymentTrend) {

    public PaymentAnalytics {
        if (totalPaymentVolumePaise < 0) {
            throw new IllegalArgumentException("totalPaymentVolumePaise must not be negative");
        }
        if (verifiedPaymentVolumePaise < 0) {
            throw new IllegalArgumentException("verifiedPaymentVolumePaise must not be negative");
        }
        if (failedPaymentCount < 0) {
            throw new IllegalArgumentException("failedPaymentCount must not be negative");
        }
        if (pendingPaymentCount < 0) {
            throw new IllegalArgumentException("pendingPaymentCount must not be negative");
        }
        if (paymentSuccessRate < 0 || paymentSuccessRate > 1) {
            throw new IllegalArgumentException("paymentSuccessRate must be between 0 and 1");
        }
        if (paymentFailureRate < 0 || paymentFailureRate > 1) {
            throw new IllegalArgumentException("paymentFailureRate must be between 0 and 1");
        }
        paymentTrend = List.copyOf(Objects.requireNonNull(paymentTrend, "paymentTrend must not be null"));
    }
}
