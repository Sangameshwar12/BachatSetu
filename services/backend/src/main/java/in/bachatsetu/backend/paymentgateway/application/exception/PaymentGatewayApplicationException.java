package in.bachatsetu.backend.paymentgateway.application.exception;

/** Base type for every Payment Gateway application-layer failure. */
public class PaymentGatewayApplicationException extends RuntimeException {

    public PaymentGatewayApplicationException(String message) {
        super(message);
    }
}
