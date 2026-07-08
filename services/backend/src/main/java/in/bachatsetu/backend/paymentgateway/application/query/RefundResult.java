package in.bachatsetu.backend.paymentgateway.application.query;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;
import java.util.UUID;

/** Application view returned after a refund is initiated with the provider. */
public record RefundResult(
        UUID paymentId,
        GatewayType provider,
        String providerRefundId,
        boolean successful) {

    public RefundResult {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerRefundId, "provider refund id must not be null");
    }
}
