package in.bachatsetu.backend.payment.application.exception;

/** Raised when a tenant-scoped payment lookup has no result. */
public final class PaymentNotFoundException extends PaymentApplicationException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
