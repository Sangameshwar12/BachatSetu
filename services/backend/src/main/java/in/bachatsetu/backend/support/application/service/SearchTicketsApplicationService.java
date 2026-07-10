package in.bachatsetu.backend.support.application.service;

import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.SearchTicketsUseCase;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;
import java.util.Objects;

public final class SearchTicketsApplicationService implements SearchTicketsUseCase {

    private final SupportTicketRepository repository;
    private final TransactionPort transaction;
    private final SupportApplicationMapper mapper;

    public SearchTicketsApplicationService(
            SupportTicketRepository repository, TransactionPort transaction, SupportApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Page<SupportTicketResult> execute(SupportTicketSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        return transaction.execute(() -> {
            Page<SupportTicket> page = repository.search(criteria);
            return new Page<>(
                    page.content().stream().map(mapper::toResult).toList(), page.page(), page.size(),
                    page.totalElements());
        });
    }
}
