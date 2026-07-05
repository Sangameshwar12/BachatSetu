package in.bachatsetu.backend.auth.application.security;

/** Raised when application behavior requires an authenticated request identity. */
public final class CurrentUserUnavailableException extends RuntimeException {

    public CurrentUserUnavailableException() {
        super("no authenticated user is available");
    }
}
