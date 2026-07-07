package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of a receipt returned by the REST API. */
public record ReceiptResponse(

        @Schema(description = "Receipt identifier") String receiptId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the payment the receipt was generated for") String paymentId,
        @Schema(description = "Identifier of the paying member") String memberId,
        @Schema(description = "Human-facing receipt number", example = "RCT/20260807/1A2B3C4D") String number,
        @Schema(description = "Line items that make up the receipt total") List<ReceiptLineResponse> lines,
        @Schema(description = "Total receipt amount in paise") long totalAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Lifecycle status", example = "GENERATED") String status,
        @Schema(description = "Cancellation reason, once cancelled") String cancellationReason,
        @Schema(description = "Timestamp the receipt was generated") Instant generatedAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Optimistic-lock version") long version) {

    public ReceiptResponse {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
    }
}
