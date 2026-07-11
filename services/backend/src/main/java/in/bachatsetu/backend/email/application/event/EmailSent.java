package in.bachatsetu.backend.email.application.event;

import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Published by the email delivery infrastructure once a message is successfully handed to a
 * provider. {@code maskedRecipient} carries only a masked form of the destination address (never
 * the full address) so Audit can record delivery diagnostics without exposing PII, mirroring how
 * {@code OtpSendFailed} carries no phone number at all.
 */
public record EmailSent(
        UUID eventId,
        String maskedRecipient,
        EmailTemplateCategory category,
        String provider,
        String providerMessageId,
        long durationMillis,
        Instant occurredAt) {

    public EmailSent {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(maskedRecipient, "maskedRecipient must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerMessageId, "providerMessageId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
