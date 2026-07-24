package in.bachatsetu.backend.draw.domain.exception;

/**
 * Raised when a persistence-layer write for a draw conflicts with another write for the same
 * draw or cycle — e.g. two concurrent requests to schedule a draw for the same cycle (the
 * database's {@code uk_draws_cycle} unique constraint allows only one), or two concurrent
 * conduct/close requests against the same draw racing past each other's optimistic-lock version.
 */
public final class DrawConflictException extends RuntimeException {

    public DrawConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
