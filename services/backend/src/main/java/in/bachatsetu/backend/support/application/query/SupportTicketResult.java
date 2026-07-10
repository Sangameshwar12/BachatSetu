package in.bachatsetu.backend.support.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import java.time.Instant;

public record SupportTicketResult(
        AggregateId ticketId,
        AggregateId tenantId,
        AggregateId raisedBy,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String subject,
        String description,
        AggregateId assignedTo,
        Instant createdAt,
        Instant resolvedAt,
        String resolution) {
}
