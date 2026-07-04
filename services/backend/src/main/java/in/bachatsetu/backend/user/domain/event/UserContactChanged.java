package in.bachatsetu.backend.user.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.user.domain.model.UserContact;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserContactChanged(
        UUID eventId,
        AggregateId aggregateId,
        UserContact previousContact,
        UserContact currentContact,
        Instant occurredAt) implements DomainEvent {

    public UserContactChanged {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(previousContact, "previousContact must not be null");
        Objects.requireNonNull(currentContact, "currentContact must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
