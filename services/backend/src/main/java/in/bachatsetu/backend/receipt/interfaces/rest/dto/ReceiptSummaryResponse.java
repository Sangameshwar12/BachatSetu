package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Compact presentation view of a receipt returned by list endpoints. */
public record ReceiptSummaryResponse(

        @Schema(description = "Receipt identifier") String receiptId,
        @Schema(description = "Human-facing receipt number", example = "RCT/20260807/1A2B3C4D") String number,
        @Schema(description = "Total receipt amount in paise") long totalAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Lifecycle status", example = "GENERATED") String status,
        @Schema(description = "Timestamp the receipt was generated") Instant generatedAt) {
}
