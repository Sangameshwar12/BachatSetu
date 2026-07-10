package in.bachatsetu.backend.auth.application.signup.exception;

import java.util.Objects;

/** Signals a signup attempt was rejected for a specific, recognized business reason. */
public final class SignupApplicationException extends RuntimeException {

    private final SignupFailureReason reason;

    public SignupApplicationException(SignupFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public SignupFailureReason reason() {
        return reason;
    }
}
