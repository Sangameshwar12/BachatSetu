package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request to generate a new receipt for a verified payment. */
public record CreateReceiptRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the payment the receipt is generated for", example = "123e4567-e89b-12d3-a456-426614174000")
        String paymentId,

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the paying member", example = "123e4567-e89b-12d3-a456-426614174000")
        String memberId,

        @NotEmpty
        @Valid
        @Schema(description = "Line items that make up the receipt total")
        List<ReceiptLineRequest> lines) {

    public CreateReceiptRequest {
        lines = lines == null ? null : List.copyOf(lines);
    }
}
