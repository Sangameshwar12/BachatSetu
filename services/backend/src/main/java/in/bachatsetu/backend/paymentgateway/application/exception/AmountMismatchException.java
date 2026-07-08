package in.bachatsetu.backend.paymentgateway.application.exception;

/** Thrown when a caller-confirmed order amount does not match the payment's own recorded amount. */
public final class AmountMismatchException extends PaymentGatewayApplicationException {

    public AmountMismatchException(String message) {
        super(message);
    }
}
