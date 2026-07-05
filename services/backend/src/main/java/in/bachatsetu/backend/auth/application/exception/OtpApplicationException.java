package in.bachatsetu.backend.auth.application.exception;

import java.util.Objects;

public final class OtpApplicationException extends RuntimeException {

    private final OtpFailureReason reason;

    public OtpApplicationException(OtpFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "OTP failure reason must not be null");
    }

    public OtpFailureReason reason() {
        return reason;
    }
}
