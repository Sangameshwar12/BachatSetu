package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted when an OTP verification is generated; the OTP value is deliberately excluded. */
public record OtpGenerated(
        UUID eventId,
        AggregateId aggregateId,
        UserId userId,
        OtpPurpose purpose,
        Instant expiresAt,
        Instant occurredAt) implements DomainEvent {

    public OtpGenerated {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!expiresAt.isAfter(occurredAt)) {
            throw new IllegalArgumentException("OTP expiry must follow generation time");
        }
    }
}
