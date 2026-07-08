package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;

/** Verifies the {@code x-webhook-signature} header using the configured Cashfree webhook secret. */
public final class CashfreeWebhookVerifier implements PaymentWebhookVerifierPort {

    private final String webhookSecret;

    public CashfreeWebhookVerifier(String webhookSecret) {
        this.webhookSecret = Objects.requireNonNull(webhookSecret, "webhook secret must not be null");
    }

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.CASHFREE;
    }

    @Override
    public boolean verifySignature(String rawPayload, String signatureHeader) {
        return HmacSha256Signer.matches(rawPayload, webhookSecret, signatureHeader);
    }
}
