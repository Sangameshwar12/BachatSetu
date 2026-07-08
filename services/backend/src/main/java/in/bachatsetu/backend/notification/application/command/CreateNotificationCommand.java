package in.bachatsetu.backend.notification.application.command;

import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Map;
import java.util.Objects;

/** Requests creation and immediate synchronous dispatch of a new Notification. */
public record CreateNotificationCommand(
        AggregateId tenantId,
        AggregateId recipientUserId,
        String destination,
        NotificationChannel channel,
        NotificationCategory category,
        Map<String, String> placeholders,
        AggregateId actorId) {

    public CreateNotificationCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(recipientUserId, "recipient user id must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(category, "category must not be null");
        placeholders = Map.copyOf(Objects.requireNonNull(placeholders, "placeholders must not be null"));
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
