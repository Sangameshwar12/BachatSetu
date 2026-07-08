package in.bachatsetu.backend.paymentgateway.application.exception;

public final class GatewayOrderNotFoundException extends PaymentGatewayApplicationException {

    public GatewayOrderNotFoundException(String message) {
        super(message);
    }
}
