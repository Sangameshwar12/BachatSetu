package in.bachatsetu.backend.infrastructure.auth.sms;

/**
 * Raised by an {@link SmsProviderClient} when a send attempt fails. {@code retryable} decides
 * whether {@link SmsOtpSenderAdapter} attempts another try: timeouts, network interruptions,
 * and HTTP 502/503/504 are retryable; a provider-rejected request (400/401/403/404, or any
 * other client error) is not — retrying a request the provider has already rejected as invalid
 * cannot succeed and only wastes the retry budget. The message is safe to include in logs (no
 * OTP, no phone number, no API key) but is never surfaced to a REST caller directly — the
 * adapter always translates this into the existing {@code OtpApplicationException}.
 */
public final class SmsProviderException extends RuntimeException {

    private final boolean retryable;
    private final int httpStatus;

    public SmsProviderException(String message, boolean retryable, int httpStatus) {
        super(message);
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public SmsProviderException(String message, boolean retryable, int httpStatus, Throwable cause) {
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
