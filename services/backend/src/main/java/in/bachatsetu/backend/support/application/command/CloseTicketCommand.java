package in.bachatsetu.backend.support.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record CloseTicketCommand(AggregateId ticketId, AggregateId actorId) {

    public CloseTicketCommand {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
