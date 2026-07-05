package in.bachatsetu.backend.auth.application.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Sensitive-data-free event returned by OTP application use cases. */
public interface OtpApplicationEvent {

    UUID eventId();

    AggregateId verificationId();

    UserId userId();

    OtpPurpose purpose();

    Instant occurredAt();

    static void validate(
            UUID eventId,
            AggregateId verificationId,
            UserId userId,
            OtpPurpose purpose,
            Instant occurredAt) {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(verificationId, "verification id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
