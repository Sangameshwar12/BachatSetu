package in.bachatsetu.backend.paymentgateway.application.query;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;
import java.util.UUID;

/**
 * Application view of a payment's gateway-side status, as last observed via a webhook or an explicit sync.
 * {@code successful}/{@code failed} are this module's own normalized read of {@code providerStatus} — never
 * the other way around — so application code never needs to know each provider's raw status vocabulary.
 */
public record PaymentStatusResult(
        UUID paymentId,
        GatewayType provider,
        String providerOrderId,
        String providerStatus,
        boolean successful,
        boolean failed) {

    public PaymentStatusResult {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerOrderId, "provider order id must not be null");
        Objects.requireNonNull(providerStatus, "provider status must not be null");
        if (successful && failed) {
            throw new IllegalArgumentException("a status result cannot be both successful and failed");
        }
    }
}
