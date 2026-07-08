package in.bachatsetu.backend.paymentgateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** REST view of a payment's most recently observed gateway-side status. */
public record PaymentStatusResponse(

        @Schema(description = "Gateway provider", example = "RAZORPAY")
        String provider,

        @Schema(description = "The provider's own order identifier")
        String providerOrderId,

        @Schema(description = "The provider's own raw status string", example = "captured")
        String providerStatus,

        @Schema(description = "Whether the provider reports the payment as successful")
        boolean successful,

        @Schema(description = "Whether the provider reports the payment as failed")
        boolean failed) {
}
