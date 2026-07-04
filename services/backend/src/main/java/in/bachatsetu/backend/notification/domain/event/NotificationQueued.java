package in.bachatsetu.backend.notification.domain.event;

import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NotificationQueued(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId recipientUserId,
        NotificationChannel channel,
        NotificationCategory category,
        Instant scheduledAt,
        Instant occurredAt) implements DomainEvent {

    public NotificationQueued {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
