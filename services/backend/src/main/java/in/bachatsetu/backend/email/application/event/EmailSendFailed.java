package in.bachatsetu.backend.email.application.event;

import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Published by the email delivery infrastructure when every configured retry attempt has been
 * exhausted. Contains no email body and no provider secret — only diagnostic metadata safe to
 * persist as an audit entry.
 */
public record EmailSendFailed(
        UUID eventId,
        String maskedRecipient,
        EmailTemplateCategory category,
        String provider,
        String failureReason,
        long durationMillis,
        Instant occurredAt) {

    public EmailSendFailed {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(maskedRecipient, "maskedRecipient must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
