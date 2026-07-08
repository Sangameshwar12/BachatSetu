package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;

/**
 * Verifies the {@code Stripe-Signature} header using the configured Stripe webhook secret.
 *
 * <p>Real Stripe signatures are a composite header ({@code t=<timestamp>,v1=<hmac>}) signed over
 * {@code "<timestamp>.<payload>"} with timestamp-tolerance replay protection. This simplified verifier
 * treats the entire header value as the HMAC-SHA256 hex digest of the raw payload alone, matching this
 * module's own simulated adapters rather than Stripe's real webhook format — see
 * {@code docs/application/payment-gateway.md} for why.
 */
public final class StripeWebhookVerifier implements PaymentWebhookVerifierPort {

    private final String webhookSecret;

    public StripeWebhookVerifier(String webhookSecret) {
        this.webhookSecret = Objects.requireNonNull(webhookSecret, "webhook secret must not be null");
    }

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.STRIPE;
    }

    @Override
    public boolean verifySignature(String rawPayload, String signatureHeader) {
        return HmacSha256Signer.matches(rawPayload, webhookSecret, signatureHeader);
    }
}
