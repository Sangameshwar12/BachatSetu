package in.bachatsetu.backend.draw.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that an open draw be closed with a winning member. */
public record CloseDrawCommand(AggregateId tenantId, AggregateId drawId, AggregateId winnerId, AggregateId actorId) {

    public CloseDrawCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(winnerId, "winner id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
