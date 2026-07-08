package in.bachatsetu.backend.paymentgateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** REST view of a created (or previously created) gateway order. */
public record PaymentOrderResponse(

        @Schema(description = "Payment identifier the order was created for")
        String paymentId,

        @Schema(description = "Gateway provider", example = "RAZORPAY")
        String provider,

        @Schema(description = "The provider's own order identifier")
        String providerOrderId,

        @Schema(description = "A hosted payment link, if the provider returned one")
        String paymentLink) {
}
