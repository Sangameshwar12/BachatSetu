package in.bachatsetu.backend.infrastructure.auth.sms;

/**
 * A single SMS provider's send operation, isolated from OTP business rules and from every other
 * provider. {@link SmsOtpSenderAdapter} is the only caller, and never knows which implementation
 * is active — swapping providers is a configuration change ({@code SMS_PROVIDER}), never a code
 * change to any caller.
 */
@FunctionalInterface
public interface SmsProviderClient {

    /**
     * Sends one message. Throws {@link SmsProviderException} on any failure — never returns a
     * partial/ambiguous result, so the caller's retry decision is driven entirely by
     * {@link SmsProviderException#retryable()}.
     */
    SmsSendResult send(SmsMessage message);
}
