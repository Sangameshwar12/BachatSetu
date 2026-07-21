package in.bachatsetu.backend.payment.application.exception;

/** Raised when a collection operation targets a group that does not exist in the caller's tenant. */
public final class CollectionGroupNotFoundException extends PaymentApplicationException {

    public CollectionGroupNotFoundException(String message) {
        super(message);
    }
}
