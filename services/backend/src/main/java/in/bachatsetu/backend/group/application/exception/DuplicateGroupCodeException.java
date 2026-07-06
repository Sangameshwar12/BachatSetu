package in.bachatsetu.backend.group.application.exception;

/** Raised when a generated group code is already reserved inside the tenant. */
public final class DuplicateGroupCodeException extends SavingsGroupApplicationException {

    public DuplicateGroupCodeException(String message) {
        super(message);
    }
}
