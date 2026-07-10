package in.bachatsetu.backend.invitation.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InvitationCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId groupId,
        Instant occurredAt) implements DomainEvent {

    public InvitationCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
