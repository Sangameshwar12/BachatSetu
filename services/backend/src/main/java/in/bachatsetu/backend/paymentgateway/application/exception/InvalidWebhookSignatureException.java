package in.bachatsetu.backend.paymentgateway.application.exception;

/** Thrown when a webhook's signature does not verify against the configured provider secret. */
public final class InvalidWebhookSignatureException extends PaymentGatewayApplicationException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
