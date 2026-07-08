package in.bachatsetu.backend.notification.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Compact notification view optimized for list use cases. */
public record NotificationSummary(
        UUID notificationId,
        String channel,
        String category,
        String status,
        Instant scheduledAt,
        Instant createdAt) {

    public NotificationSummary {
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
        Objects.requireNonNull(createdAt, "created at must not be null");
    }
}
