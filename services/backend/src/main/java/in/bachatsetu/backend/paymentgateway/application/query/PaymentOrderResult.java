package in.bachatsetu.backend.paymentgateway.application.query;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;
import java.util.UUID;

/** Application view returned after a gateway order is created for a payment. */
public record PaymentOrderResult(
        UUID paymentId,
        GatewayType provider,
        String providerOrderId,
        String paymentLink) {

    public PaymentOrderResult {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
    }
}
