package in.bachatsetu.backend.infrastructure.email;

/**
 * A single email provider's send operation, isolated from every other provider. {@link
 * RetryingEmailSenderAdapter} is the only caller and never knows which implementation is active —
 * swapping providers is a configuration change ({@code EMAIL_PROVIDER}), never a code change.
 */
@FunctionalInterface
public interface EmailProviderClient {

    /**
     * Sends one message. Throws {@link EmailProviderException} on any failure — never returns a
     * partial/ambiguous result, so the caller's retry decision is driven entirely by {@link
     * EmailProviderException#retryable()}.
     */
    EmailProviderSendResult send(EmailProviderMessage message);
}
