package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;

@FunctionalInterface
public interface SearchTicketsUseCase {

    Page<SupportTicketResult> execute(SupportTicketSearchCriteria criteria);
}
