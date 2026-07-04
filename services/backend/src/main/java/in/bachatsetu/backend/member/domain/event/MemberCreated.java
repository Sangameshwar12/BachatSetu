package in.bachatsetu.backend.member.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MemberCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId tenantId,
        AggregateId userId,
        Instant occurredAt) implements DomainEvent {

    public MemberCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
