package in.bachatsetu.backend.email.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Infrastructure-only delivery metadata: which provider handled the request, the provider's own
 * message identifier (for support/troubleshooting correlation), and when the attempt completed.
 * {@code providerMessageId} is {@code null} when {@code status} is {@link
 * EmailDeliveryStatus#FAILED}; {@code failureReason} is {@code null} when {@code status} is
 * {@link EmailDeliveryStatus#SENT}. Never contains the email body or a provider secret. This is
 * not a business entity — nothing here is persisted; it exists purely as audit metadata and the
 * caller's own return value.
 */
public record EmailSendResult(
        EmailDeliveryStatus status,
        String provider,
        String providerMessageId,
        Instant sentAt,
        String failureReason) {

    public EmailSendResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(sentAt, "sentAt must not be null");
        if (status == EmailDeliveryStatus.SENT && providerMessageId == null) {
            throw new IllegalArgumentException("a SENT result must carry a provider message id");
        }
        if (status == EmailDeliveryStatus.FAILED && failureReason == null) {
            throw new IllegalArgumentException("a FAILED result must carry a failure reason");
        }
    }
}
