package in.bachatsetu.backend.platformoperations.application.command;

import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** {@code tenantId} is required when {@code scope} is {@link BroadcastScope#TENANT}, otherwise ignored. */
public record SendBroadcastNotificationCommand(
        BroadcastScope scope, AggregateId tenantId, String title, String message, AggregateId actorId) {

    public SendBroadcastNotificationCommand {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (scope == BroadcastScope.TENANT && tenantId == null) {
            throw new IllegalArgumentException("tenantId is required when scope is TENANT");
        }
    }
}
