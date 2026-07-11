package in.bachatsetu.backend.infrastructure.email;

/**
 * Raised by an {@link EmailProviderClient} when a send attempt fails. {@code retryable} decides
 * whether {@link RetryingEmailSenderAdapter} attempts another try: timeouts, network
 * interruptions, and HTTP 502/503/504 are retryable; a provider-rejected request (400/401/403/404,
 * or any other client error / logical rejection) is not. The message is safe to log (no email
 * body, no recipient address, no API key).
 */
public final class EmailProviderException extends RuntimeException {

    private final boolean retryable;
    private final int httpStatus;

    public EmailProviderException(String message, boolean retryable, int httpStatus) {
        super(message);
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public EmailProviderException(String message, boolean retryable, int httpStatus, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public boolean retryable() {
        return retryable;
    }

    /** The provider's HTTP status, or {@code -1} for a failure with no HTTP response (timeout, network error). */
    public int httpStatus() {
        return httpStatus;
    }
}
