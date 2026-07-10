package in.bachatsetu.backend.support.interfaces.rest.dto;

import java.time.Instant;

public record TicketResponse(
        String ticketId,
        String tenantId,
        String raisedBy,
        String category,
        String priority,
        String status,
        String subject,
        String description,
        String assignedTo,
        Instant createdAt,
        Instant resolvedAt,
        String resolution) {
}
