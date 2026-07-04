package in.bachatsetu.backend.notification.domain.event;

import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NotificationStatusChanged(
        UUID eventId,
        AggregateId aggregateId,
        NotificationStatus previousStatus,
        NotificationStatus currentStatus,
        Instant occurredAt) implements DomainEvent {

    public NotificationStatusChanged {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
