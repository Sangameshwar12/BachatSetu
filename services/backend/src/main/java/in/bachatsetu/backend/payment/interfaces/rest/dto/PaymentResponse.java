package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of a payment returned by the REST API. */
public record PaymentResponse(

        @Schema(description = "Payment identifier") String paymentId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the group the payment belongs to") String groupId,
        @Schema(description = "Identifier of the paying member") String memberId,
        @Schema(description = "Human-facing payment reference", example = "PAY-1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D") String reference,
        @Schema(description = "Payment amount in paise") long amountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Payment method", example = "UPI") String method,
        @Schema(description = "Lifecycle status", example = "INITIATED") String status,
        @Schema(description = "Reconciliation status", example = "NOT_REQUIRED") String reconciliationStatus,
        @Schema(description = "Attempts made against this payment") List<PaymentAttemptResponse> attempts,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Optimistic-lock version") long version) {

    public PaymentResponse {
        attempts = List.copyOf(Objects.requireNonNull(attempts, "attempts must not be null"));
    }
}
