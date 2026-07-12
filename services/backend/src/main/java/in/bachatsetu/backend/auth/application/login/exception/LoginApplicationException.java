package in.bachatsetu.backend.auth.application.login.exception;

import java.util.Objects;

/** Signals a login attempt was rejected for a specific, recognized business reason. */
public final class LoginApplicationException extends RuntimeException {

    private final LoginFailureReason reason;

    public LoginApplicationException(LoginFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public LoginFailureReason reason() {
        return reason;
    }
}
