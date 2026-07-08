package in.bachatsetu.backend.paymentgateway.application.port;

import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;

/**
 * Verifies that an inbound webhook request genuinely originated from the provider it claims to, before any
 * of its content is trusted. One implementation exists per {@link GatewayType}.
 */
public interface PaymentWebhookVerifierPort {

    GatewayType supportedProvider();

    /**
     * @param rawPayload the exact, unmodified request body bytes (as a string) the signature was computed
     *                    over — never a re-serialized or reformatted representation, which would not
     *                    reproduce the same signature.
     * @param signatureHeader the value of the provider's signature header for this request.
     */
    boolean verifySignature(String rawPayload, String signatureHeader);
}
