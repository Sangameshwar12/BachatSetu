package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Compact presentation view of a payment returned by list endpoints. */
public record PaymentSummaryResponse(

        @Schema(description = "Payment identifier") String paymentId,
        @Schema(description = "Human-facing payment reference", example = "PAY-1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D") String reference,
        @Schema(description = "Payment amount in paise") long amountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Payment method", example = "UPI") String method,
        @Schema(description = "Lifecycle status", example = "INITIATED") String status,
        @Schema(description = "Creation timestamp") Instant createdAt) {
}
