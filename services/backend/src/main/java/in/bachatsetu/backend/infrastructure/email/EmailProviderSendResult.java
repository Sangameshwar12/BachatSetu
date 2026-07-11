package in.bachatsetu.backend.infrastructure.email;

import java.time.Instant;
import java.util.Objects;

/** Infrastructure-only delivery metadata captured from a provider's response. Never contains the email body. */
public record EmailProviderSendResult(String providerName, String providerMessageId, Instant sentAt) {

    public EmailProviderSendResult {
        Objects.requireNonNull(providerName, "provider name must not be null");
        Objects.requireNonNull(providerMessageId, "provider message id must not be null");
        Objects.requireNonNull(sentAt, "sentAt must not be null");
    }
}
