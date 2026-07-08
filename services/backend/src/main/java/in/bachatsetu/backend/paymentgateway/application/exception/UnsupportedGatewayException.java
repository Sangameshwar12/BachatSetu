package in.bachatsetu.backend.paymentgateway.application.exception;

/** Thrown when no adapter is registered for a requested {@code GatewayType}. */
public final class UnsupportedGatewayException extends PaymentGatewayApplicationException {

    public UnsupportedGatewayException(String message) {
        super(message);
    }
}
