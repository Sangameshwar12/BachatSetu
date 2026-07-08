package in.bachatsetu.backend.notification.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of a Notification aggregate. */
public record NotificationResult(
        UUID notificationId,
        UUID tenantId,
        UUID recipientUserId,
        String destination,
        String channel,
        String category,
        String subject,
        String body,
        String status,
        Instant scheduledAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt,
        String failureReason,
        long version) {

    public NotificationResult {
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(recipientUserId, "recipient user id must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
        Objects.requireNonNull(createdAt, "created at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
    }
}
