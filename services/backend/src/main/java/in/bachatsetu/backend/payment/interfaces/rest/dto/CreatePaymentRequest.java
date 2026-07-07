package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request to initiate a new payment. */
public record CreatePaymentRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the group the payment belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
        String groupId,

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the paying member", example = "123e4567-e89b-12d3-a456-426614174000")
        String memberId,

        @NotBlank
        @Size(min = 16, max = 128)
        @Schema(description = "Client-supplied idempotency key", example = "checkout-attempt-0001")
        String idempotencyKey,

        @Positive
        @Schema(description = "Payment amount in paise", example = "500000")
        long amountPaise,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.PAYMENT_METHOD)
        @Schema(description = "Payment method", example = "UPI")
        String method) {
}
