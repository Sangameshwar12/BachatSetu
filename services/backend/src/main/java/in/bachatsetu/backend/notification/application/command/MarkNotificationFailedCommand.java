package in.bachatsetu.backend.notification.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that an existing notification be marked failed. */
public record MarkNotificationFailedCommand(
        AggregateId tenantId, AggregateId notificationId, String failureCode, AggregateId actorId) {

    public MarkNotificationFailedCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(failureCode, "failure code must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
