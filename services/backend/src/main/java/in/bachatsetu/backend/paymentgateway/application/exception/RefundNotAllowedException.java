package in.bachatsetu.backend.paymentgateway.application.exception;

/** Thrown when a refund is requested for a payment that is not currently {@code VERIFIED}. */
public final class RefundNotAllowedException extends PaymentGatewayApplicationException {

    public RefundNotAllowedException(String message) {
        super(message);
    }
}
