package in.bachatsetu.backend.member.domain.event;

import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MemberStatusChanged(
        UUID eventId,
        AggregateId aggregateId,
        MemberStatus previousStatus,
        MemberStatus newStatus,
        Instant occurredAt) implements DomainEvent {

    public MemberStatusChanged {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
