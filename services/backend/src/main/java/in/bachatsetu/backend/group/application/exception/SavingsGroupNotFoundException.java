package in.bachatsetu.backend.group.application.exception;

/** Raised when a tenant-scoped savings group lookup has no result. */
public final class SavingsGroupNotFoundException extends SavingsGroupApplicationException {

    public SavingsGroupNotFoundException(String message) {
        super(message);
    }
}
