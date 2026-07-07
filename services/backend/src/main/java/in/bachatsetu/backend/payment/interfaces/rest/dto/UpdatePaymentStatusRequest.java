package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request to transition a payment to a new lifecycle status. */
public record UpdatePaymentStatusRequest(

        @NotBlank
        @Pattern(regexp = ValidationPatterns.UPDATABLE_PAYMENT_STATUS)
        @Schema(description = "Target lifecycle status", example = "VERIFIED")
        String status,

        @Schema(description = "Payment provider name; required when status is VERIFIED", example = "razorpay")
        String provider,

        @Schema(description = "Provider transaction identifier; required when status is VERIFIED", example = "txn_001")
        String transactionId,

        @Schema(description = "Failure code; required when status is FAILED", example = "provider-declined")
        String failureCode) {
}
