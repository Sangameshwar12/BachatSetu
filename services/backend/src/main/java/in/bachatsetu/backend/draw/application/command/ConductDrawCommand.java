package in.bachatsetu.backend.draw.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that a scheduled draw be opened and conducted. */
public record ConductDrawCommand(AggregateId tenantId, AggregateId drawId, AggregateId actorId) {

    public ConductDrawCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
