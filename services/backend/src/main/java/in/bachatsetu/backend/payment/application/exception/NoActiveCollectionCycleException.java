package in.bachatsetu.backend.payment.application.exception;

/** Raised when a collection operation requires a currently open contribution cycle and there isn't one. */
public final class NoActiveCollectionCycleException extends PaymentApplicationException {

    public NoActiveCollectionCycleException(String message) {
        super(message);
    }
}
