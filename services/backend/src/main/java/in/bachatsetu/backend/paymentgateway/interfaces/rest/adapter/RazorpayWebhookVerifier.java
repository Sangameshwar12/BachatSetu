package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import in.bachatsetu.backend.paymentgateway.application.port.PaymentWebhookVerifierPort;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import java.util.Objects;

/** Verifies the {@code X-Razorpay-Signature} header using the configured Razorpay webhook secret. */
public final class RazorpayWebhookVerifier implements PaymentWebhookVerifierPort {

    private final String webhookSecret;

    public RazorpayWebhookVerifier(String webhookSecret) {
        this.webhookSecret = Objects.requireNonNull(webhookSecret, "webhook secret must not be null");
    }

    @Override
    public GatewayType supportedProvider() {
        return GatewayType.RAZORPAY;
    }

    @Override
    public boolean verifySignature(String rawPayload, String signatureHeader) {
        return HmacSha256Signer.matches(rawPayload, webhookSecret, signatureHeader);
    }
}
