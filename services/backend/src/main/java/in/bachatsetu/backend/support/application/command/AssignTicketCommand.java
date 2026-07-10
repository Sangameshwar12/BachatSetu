package in.bachatsetu.backend.support.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record AssignTicketCommand(AggregateId ticketId, AggregateId assigneeId, AggregateId actorId) {

    public AssignTicketCommand {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(assigneeId, "assigneeId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
