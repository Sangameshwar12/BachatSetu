package in.bachatsetu.backend.draw.application.exception;

/** Raised when a draw is scheduled, conducted, or closed for a group that is not currently active. */
public final class GroupNotActiveException extends DrawApplicationException {

    public GroupNotActiveException(String message) {
        super(message);
    }
}
