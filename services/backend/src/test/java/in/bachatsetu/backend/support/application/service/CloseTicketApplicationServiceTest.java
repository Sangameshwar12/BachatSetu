package in.bachatsetu.backend.support.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.command.CloseTicketCommand;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CloseTicketApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final SupportApplicationMapper mapper = new SupportApplicationMapper();
    private final CloseTicketApplicationService service =
            new CloseTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);

    @Test
    void closesAResolvedTicket() {
        AggregateId raisedBy = AggregateId.newId();
        AggregateId ticketId = AggregateId.newId();
        AggregateId admin = AggregateId.newId();
        SupportTicket ticket = SupportTicket.create(
                ticketId, AggregateId.newId(), raisedBy, TicketCategory.STORAGE, TicketPriority.LOW, "Subject",
                "Description", raisedBy, NOW);
        ticket.assign(admin, admin, NOW.plusSeconds(30));
        ticket.resolve("Done", admin, NOW.plusSeconds(60));
        when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(clock.now()).thenReturn(NOW.plusSeconds(120));

        SupportTicketResult result = service.execute(new CloseTicketCommand(ticketId, admin));

        assertThat(result.status()).isEqualTo(TicketStatus.CLOSED);
        verify(repository).save(any(SupportTicket.class));
        verify(eventPublisher).publish(any());
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
