package in.bachatsetu.backend.group.application.exception;

/** Raised when the acting user is not authorized to perform a group operation. */
public final class GroupAccessDeniedException extends SavingsGroupApplicationException {

    public GroupAccessDeniedException(String message) {
        super(message);
    }
}
