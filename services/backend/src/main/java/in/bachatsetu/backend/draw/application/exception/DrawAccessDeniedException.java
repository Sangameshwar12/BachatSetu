package in.bachatsetu.backend.draw.application.exception;

/** Raised when the acting user is not authorized to perform a draw operation. */
public final class DrawAccessDeniedException extends DrawApplicationException {

    public DrawAccessDeniedException(String message) {
        super(message);
    }
}
