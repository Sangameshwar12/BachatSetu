package in.bachatsetu.backend.auth.application.token.exception;

import java.util.Objects;

public final class TokenApplicationException extends RuntimeException {

    private final TokenFailureReason reason;

    public TokenApplicationException(TokenFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "token failure reason must not be null");
    }

    public TokenFailureReason reason() {
        return reason;
    }
}
