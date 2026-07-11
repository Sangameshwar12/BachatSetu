package in.bachatsetu.backend.infrastructure.auth.sms;

import java.time.Instant;
import java.util.Objects;

/**
 * Infrastructure-only delivery metadata captured from a provider's response: which provider
 * handled the request, the provider's own message identifier (for support/troubleshooting
 * correlation with that provider's dashboard), and when the attempt completed. Never contains
 * the OTP code, the destination number, or a provider secret. This is not an OTP business
 * concept — nothing here is persisted or exposed through the OTP domain model or REST API.
 */
public record SmsSendResult(String providerName, String providerMessageId, Instant sentAt) {

    public SmsSendResult {
        Objects.requireNonNull(providerName, "provider name must not be null");
        Objects.requireNonNull(providerMessageId, "provider message id must not be null");
        Objects.requireNonNull(sentAt, "sentAt must not be null");
    }
}
