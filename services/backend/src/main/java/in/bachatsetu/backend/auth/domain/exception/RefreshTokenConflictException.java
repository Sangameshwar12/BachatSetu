package in.bachatsetu.backend.auth.domain.exception;

/**
 * Raised when a persistence-layer write for a refresh token conflicts with another write for the
 * same token — e.g. two concurrent refresh requests both passing the in-memory status check
 * before either commits, with the row's optimistic-lock version then rejecting the second.
 */
public final class RefreshTokenConflictException extends RuntimeException {

    public RefreshTokenConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
