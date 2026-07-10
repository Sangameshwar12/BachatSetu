package in.bachatsetu.backend.dashboard.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record RecentNotificationSummary(
        AggregateId notificationId, String category, String status, Instant createdAt) {

    public RecentNotificationSummary {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
