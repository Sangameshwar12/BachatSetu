package in.bachatsetu.backend.support.application.exception;

import java.util.Objects;

public final class SupportApplicationException extends RuntimeException {

    private final SupportFailureReason reason;

    public SupportApplicationException(SupportFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public SupportFailureReason reason() {
        return reason;
    }
}
