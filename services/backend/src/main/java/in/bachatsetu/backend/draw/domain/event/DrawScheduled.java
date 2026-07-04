package in.bachatsetu.backend.draw.domain.event;

import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DrawScheduled(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId groupId,
        AggregateId cycleId,
        DrawNumber drawNumber,
        Instant scheduledAt,
        Instant occurredAt) implements DomainEvent {

    public DrawScheduled {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(cycleId, "cycleId must not be null");
        Objects.requireNonNull(drawNumber, "drawNumber must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
