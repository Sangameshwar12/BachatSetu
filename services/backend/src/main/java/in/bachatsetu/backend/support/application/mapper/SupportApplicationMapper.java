package in.bachatsetu.backend.support.application.mapper;

import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.model.SupportTicket;

public final class SupportApplicationMapper {

    public SupportTicketResult toResult(SupportTicket ticket) {
        return new SupportTicketResult(
                ticket.id(),
                ticket.tenantId(),
                ticket.raisedBy(),
                ticket.category(),
                ticket.priority(),
                ticket.status(),
                ticket.subject(),
                ticket.description(),
                ticket.assignedTo(),
                ticket.auditInfo().createdAt(),
                ticket.resolvedAt(),
                ticket.resolution());
    }
}
