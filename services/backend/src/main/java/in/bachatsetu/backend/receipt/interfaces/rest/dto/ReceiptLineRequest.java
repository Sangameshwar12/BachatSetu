package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request for a single receipt line. */
public record ReceiptLineRequest(

        @NotBlank
        @Pattern(regexp = ValidationPatterns.RECEIPT_TYPE)
        @Schema(description = "Receipt line type", example = "CONTRIBUTION")
        String type,

        @NotBlank
        @Size(max = 200)
        @Schema(description = "Human-readable line description", example = "Monthly contribution")
        String description,

        @Positive
        @Schema(description = "Line amount in paise", example = "500000")
        long amountPaise) {
}
