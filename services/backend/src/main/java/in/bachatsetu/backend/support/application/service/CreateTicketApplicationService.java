package in.bachatsetu.backend.support.application.service;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.command.CreateTicketCommand;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.CreateTicketUseCase;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.util.Objects;

public final class CreateTicketApplicationService implements CreateTicketUseCase {

    private final SupportTicketRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final SupportApplicationMapper mapper;

    public CreateTicketApplicationService(
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
    public SupportTicketResult execute(CreateTicketCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transaction.execute(() -> {
            SupportTicket ticket = SupportTicket.create(
                    AggregateId.newId(), command.tenantId(), command.raisedBy(), command.category(),
                    command.priority(), command.subject(), command.description(), command.raisedBy(), clock.now());
            repository.save(ticket);
            eventPublisher.publish(ticket.pullDomainEvents());
            return mapper.toResult(ticket);
        });
    }
}
