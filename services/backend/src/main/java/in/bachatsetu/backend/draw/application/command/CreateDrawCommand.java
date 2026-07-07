package in.bachatsetu.backend.draw.application.command;

import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Requests scheduling of a new Draw aggregate. */
public record CreateDrawCommand(
        AggregateId tenantId,
        AggregateId groupId,
        AggregateId cycleId,
        DrawNumber number,
        DrawType type,
        Instant scheduledAt,
        AggregateId actorId) {

    public CreateDrawCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(cycleId, "cycle id must not be null");
        Objects.requireNonNull(number, "draw number must not be null");
        Objects.requireNonNull(type, "draw type must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
