package in.bachatsetu.backend.payment.application.exception;

/** Raised when a non-owner actor attempts an organizer-only collection operation. */
public final class CollectionAccessDeniedException extends PaymentApplicationException {

    public CollectionAccessDeniedException(String message) {
        super(message);
    }
}
