package in.bachatsetu.backend.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.AuditSortField;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchAuditApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private AuditRepository repository;
    private SearchAuditApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditRepository.class);
        service = new SearchAuditApplicationService(
                repository, new DirectTransactionPort(), new AuditApplicationMapper());
    }

    @Test
    void searchesAndMapsAPageOfEntries() {
        AggregateId tenantId = AggregateId.newId();
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                tenantId, null, "auth", AuditEventType.LOGIN, null, null, 0, 20, AuditSortField.CREATED_AT,
                SortDirection.DESC);
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, AggregateId.newId(), AuditEventType.LOGIN, "auth", null, null,
                "LOGIN", null, null, null, null, NOW);
        when(repository.search(criteria)).thenReturn(new AuditPage<>(List.of(entry), 0, 20, 1));

        AuditPage<AuditEntryResult> result = service.execute(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() {
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                AggregateId.newId(), null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT,
                SortDirection.DESC);
        when(repository.search(criteria)).thenReturn(new AuditPage<>(List.of(), 0, 20, 0));

        AuditPage<AuditEntryResult> result = service.execute(criteria);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsANullCriteria() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AuditApplicationMapper mapper = new AuditApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new SearchAuditApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SearchAuditApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SearchAuditApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
