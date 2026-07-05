package in.bachatsetu.backend.auth.application.token.port;

import java.util.Objects;

/** Stable validation failure emitted by a JWT provider adapter. */
public final class JwtValidationException extends RuntimeException {

    private final JwtValidationFailure failure;

    public JwtValidationException(JwtValidationFailure failure, String message) {
        super(message);
        this.failure = Objects.requireNonNull(failure, "JWT validation failure must not be null");
    }

    public JwtValidationFailure failure() {
        return failure;
    }
}
