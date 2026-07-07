package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe presentation view of one receipt line. */
public record ReceiptLineResponse(

        @Schema(description = "Line identifier") String lineId,
        @Schema(description = "Receipt line type", example = "CONTRIBUTION") String type,
        @Schema(description = "Human-readable line description") String description,
        @Schema(description = "Line amount in paise") long amountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode) {
}
