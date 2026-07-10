package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;

@FunctionalInterface
public interface GetTicketUseCase {

    SupportTicketResult execute(AggregateId ticketId);
}
