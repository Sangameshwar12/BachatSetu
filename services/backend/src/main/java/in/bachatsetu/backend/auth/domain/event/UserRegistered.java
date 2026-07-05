package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted when an authentication user is registered. */
public record UserRegistered(
        UUID eventId,
        UserId userId,
        Email email,
        MobileNumber mobileNumber,
        Instant occurredAt) implements DomainEvent {

    public UserRegistered {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    @Override
    public AggregateId aggregateId() {
        return userId.toAggregateId();
    }
}
