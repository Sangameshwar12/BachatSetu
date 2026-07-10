package in.bachatsetu.backend.invitation.domain.event;

import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted once an invitation is accepted; {@code channel} records how the joiner used it. */
public record InvitationAccepted(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId groupId,
        AggregateId acceptedBy,
        InvitationType channel,
        Instant occurredAt) implements DomainEvent {

    public InvitationAccepted {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(acceptedBy, "acceptedBy must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
