package in.bachatsetu.backend.notification.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that an existing notification be marked delivered. */
public record MarkNotificationDeliveredCommand(AggregateId tenantId, AggregateId notificationId, AggregateId actorId) {

    public MarkNotificationDeliveredCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
