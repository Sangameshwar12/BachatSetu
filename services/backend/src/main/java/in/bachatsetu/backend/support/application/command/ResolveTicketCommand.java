package in.bachatsetu.backend.support.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record ResolveTicketCommand(AggregateId ticketId, String resolution, AggregateId actorId) {

    public ResolveTicketCommand {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
