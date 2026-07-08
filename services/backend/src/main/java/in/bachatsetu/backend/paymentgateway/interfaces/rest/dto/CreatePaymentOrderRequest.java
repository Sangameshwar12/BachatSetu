package in.bachatsetu.backend.paymentgateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Confirms the amount the caller believes it is paying, validated against the payment's own recorded amount. */
public record CreatePaymentOrderRequest(

        @Positive
        @Schema(description = "Confirmed payment amount in minor units", example = "500000")
        long amountPaise,

        @NotBlank
        @Size(min = 3, max = 3)
        @Schema(description = "ISO 4217 currency code", example = "INR")
        String currencyCode) {
}
