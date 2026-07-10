package in.bachatsetu.backend.support.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.command.CreateTicketCommand;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreateTicketApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final SupportApplicationMapper mapper = new SupportApplicationMapper();
    private final CreateTicketApplicationService service =
            new CreateTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);

    @Test
    void createsAndSavesATicket() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId raisedBy = AggregateId.newId();
        when(clock.now()).thenReturn(NOW);

        SupportTicketResult result = service.execute(new CreateTicketCommand(
                tenantId, raisedBy, TicketCategory.PAYMENT, TicketPriority.HIGH, "Subject", "Description"));

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.raisedBy()).isEqualTo(raisedBy);
        verify(repository).save(any(SupportTicket.class));
        verify(eventPublisher).publish(any());
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(java.util.function.Supplier<T> operation) {
            return operation.get();
        }
    }
}
