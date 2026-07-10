package in.bachatsetu.backend.platformoperations.application.exception;

import java.util.Objects;

public final class PlatformOperationsApplicationException extends RuntimeException {

    private final PlatformOperationsFailureReason reason;

    public PlatformOperationsApplicationException(PlatformOperationsFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public PlatformOperationsFailureReason reason() {
        return reason;
    }
}
