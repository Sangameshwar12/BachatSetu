package in.bachatsetu.backend.payment.application.exception;

/** Base exception for application-level Payment failures. */
public class PaymentApplicationException extends RuntimeException {

    public PaymentApplicationException(String message) {
        super(message);
    }
}
