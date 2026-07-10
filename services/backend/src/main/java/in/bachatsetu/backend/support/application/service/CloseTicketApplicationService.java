package in.bachatsetu.backend.support.application.service;

import in.bachatsetu.backend.support.application.command.CloseTicketCommand;
import in.bachatsetu.backend.support.application.exception.SupportApplicationException;
import in.bachatsetu.backend.support.application.exception.SupportFailureReason;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.CloseTicketUseCase;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.util.Objects;

public final class CloseTicketApplicationService implements CloseTicketUseCase {

    private final SupportTicketRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final SupportApplicationMapper mapper;

    public CloseTicketApplicationService(
            SupportTicketRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SupportApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SupportTicketResult execute(CloseTicketCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transaction.execute(() -> {
            SupportTicket ticket = repository.findById(command.ticketId())
                    .orElseThrow(() -> new SupportApplicationException(
                            SupportFailureReason.TICKET_NOT_FOUND, "no support ticket exists for this identifier"));
            ticket.close(command.actorId(), clock.now());
            repository.save(ticket);
            eventPublisher.publish(ticket.pullDomainEvents());
            return mapper.toResult(ticket);
        });
    }
}
