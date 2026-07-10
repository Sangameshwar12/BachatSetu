package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Payment analytics. */
public record PaymentAnalyticsResponse(

        @Schema(description = "Total payment volume, in paise, across every payment") long totalPaymentVolumePaise,
        @Schema(description = "Total payment volume, in paise, across verified payments only")
        long verifiedPaymentVolumePaise,
        @Schema(description = "Number of failed payments") long failedPaymentCount,
        @Schema(description = "Number of pending payments (initiated or awaiting the provider)")
        long pendingPaymentCount,
        @Schema(description = "Average verified payment amount, in paise") double averageContributionPaise,
        @Schema(description = "Fraction of payments that were verified, between 0 and 1") double paymentSuccessRate,
        @Schema(description = "Fraction of payments that failed, between 0 and 1") double paymentFailureRate,
        @Schema(description = "Daily payment activity for the trailing 30 days")
        List<PaymentTrendPointResponse> paymentTrend) {

    public PaymentAnalyticsResponse {
        paymentTrend = List.copyOf(Objects.requireNonNull(paymentTrend, "paymentTrend must not be null"));
    }
}
