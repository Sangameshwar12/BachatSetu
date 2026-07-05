package in.bachatsetu.backend.auth.interfaces.rest.exception;

import java.net.URI;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/** Presentation error derived from a completed OTP use-case result. */
public final class OtpRestException extends RuntimeException {

    private final HttpStatus status;
    private final URI type;
    private final String code;

    public OtpRestException(HttpStatus status, URI type, String code, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "HTTP status must not be null");
        this.type = Objects.requireNonNull(type, "problem type must not be null");
        this.code = Objects.requireNonNull(code, "problem code must not be null");
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return type;
    }

    public String code() {
        return code;
    }
}
