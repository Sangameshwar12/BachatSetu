package in.bachatsetu.backend.support.application.service;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.exception.SupportApplicationException;
import in.bachatsetu.backend.support.application.exception.SupportFailureReason;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.util.Objects;

public final class GetTicketApplicationService implements GetTicketUseCase {

    private final SupportTicketRepository repository;
    private final TransactionPort transaction;
    private final SupportApplicationMapper mapper;

    public GetTicketApplicationService(
            SupportTicketRepository repository, TransactionPort transaction, SupportApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SupportTicketResult execute(AggregateId ticketId) {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        return transaction.execute(() -> repository.findById(ticketId)
                .map(mapper::toResult)
                .orElseThrow(() -> new SupportApplicationException(
                        SupportFailureReason.TICKET_NOT_FOUND, "no support ticket exists for this identifier")));
    }
}
