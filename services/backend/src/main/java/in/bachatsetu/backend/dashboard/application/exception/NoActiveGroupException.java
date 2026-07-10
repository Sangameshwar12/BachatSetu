package in.bachatsetu.backend.dashboard.application.exception;

/** Raised when a member has not yet joined or created any group (Welcome Screen, not a dashboard). */
public final class NoActiveGroupException extends RuntimeException {

    public NoActiveGroupException(String message) {
        super(message);
    }
}
