package in.bachatsetu.backend.notification.domain.factory;

import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class NotificationFactory {

    private final Clock clock;

    public NotificationFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Notification queue(
            AggregateId tenantId,
            NotificationRecipient recipient,
            NotificationChannel channel,
            NotificationCategory category,
            NotificationContent content,
            Instant scheduledAt,
            AggregateId actorId) {
        return Notification.queue(
                AggregateId.newId(),
                tenantId,
                recipient,
                channel,
                category,
                content,
                scheduledAt,
                actorId,
                clock.instant());
    }
}
