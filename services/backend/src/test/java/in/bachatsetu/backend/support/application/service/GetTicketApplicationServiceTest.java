package in.bachatsetu.backend.support.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.application.exception.SupportApplicationException;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetTicketApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final SupportApplicationMapper mapper = new SupportApplicationMapper();
    private final GetTicketApplicationService service =
            new GetTicketApplicationService(repository, transaction, mapper);

    @Test
    void returnsAnExistingTicket() {
        AggregateId raisedBy = AggregateId.newId();
        AggregateId ticketId = AggregateId.newId();
        SupportTicket ticket = SupportTicket.create(
                ticketId, AggregateId.newId(), raisedBy, TicketCategory.OTP, TicketPriority.MEDIUM, "Subject",
                "Description", raisedBy, NOW);
        when(repository.findById(ticketId)).thenReturn(Optional.of(ticket));

        SupportTicketResult result = service.execute(ticketId);

        assertThat(result.ticketId()).isEqualTo(ticketId);
    }

    @Test
    void rejectsAnUnknownTicket() {
        AggregateId ticketId = AggregateId.newId();
        when(repository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ticketId)).isInstanceOf(SupportApplicationException.class);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
