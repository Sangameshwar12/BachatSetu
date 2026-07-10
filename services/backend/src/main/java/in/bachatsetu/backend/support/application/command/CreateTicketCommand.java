package in.bachatsetu.backend.support.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import java.util.Objects;

public record CreateTicketCommand(
        AggregateId tenantId,
        AggregateId raisedBy,
        TicketCategory category,
        TicketPriority priority,
        String subject,
        String description) {

    public CreateTicketCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(raisedBy, "raisedBy must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
