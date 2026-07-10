package in.bachatsetu.backend.support.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.SortDirection;
import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SearchTicketsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final SupportApplicationMapper mapper = new SupportApplicationMapper();
    private final SearchTicketsApplicationService service =
            new SearchTicketsApplicationService(repository, transaction, mapper);

    @Test
    void mapsARepositoryPageToAResultPage() {
        AggregateId raisedBy = AggregateId.newId();
        SupportTicket ticket = SupportTicket.create(
                AggregateId.newId(), AggregateId.newId(), raisedBy, TicketCategory.RECEIPT, TicketPriority.LOW,
                "Subject", "Description", raisedBy, NOW);
        SupportTicketSearchCriteria criteria = new SupportTicketSearchCriteria(
                null, null, null, null, null, null, null, 0, 20, SortDirection.DESC);
        when(repository.search(criteria)).thenReturn(new Page<>(java.util.List.of(ticket), 0, 20, 1));

        Page<SupportTicketResult> result = service.execute(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
