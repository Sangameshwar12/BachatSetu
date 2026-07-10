package in.bachatsetu.backend.support.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import java.util.Optional;

/** Cross-tenant persistence boundary for {@link SupportTicket} — platform support spans every tenant. */
public interface SupportTicketRepository {

    void save(SupportTicket ticket);

    Optional<SupportTicket> findById(AggregateId ticketId);

    Page<SupportTicket> search(SupportTicketSearchCriteria criteria);
}
