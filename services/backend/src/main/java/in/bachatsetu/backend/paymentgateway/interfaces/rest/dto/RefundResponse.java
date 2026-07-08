package in.bachatsetu.backend.paymentgateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** REST view of a refund attempt. */
public record RefundResponse(

        @Schema(description = "Gateway provider", example = "RAZORPAY")
        String provider,

        @Schema(description = "The provider's own refund identifier")
        String providerRefundId,

        @Schema(description = "Whether the provider reports the refund as successful")
        boolean successful) {
}
