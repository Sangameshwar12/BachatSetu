package in.bachatsetu.backend.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.exception.AuditEntryNotFoundException;
import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAuditEntryApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private AuditRepository repository;
    private GetAuditEntryApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditRepository.class);
        service = new GetAuditEntryApplicationService(
                repository, new DirectTransactionPort(), new AuditApplicationMapper());
    }

    @Test
    void returnsAnExistingEntry() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, AggregateId.newId(), AuditEventType.LOGIN, "auth", null, null,
                "LOGIN", null, null, null, null, NOW);
        when(repository.findById(tenantId, entry.id())).thenReturn(Optional.of(entry));

        AuditEntryResult result = service.execute(tenantId, entry.id());

        assertThat(result.auditId()).isEqualTo(entry.id().value());
    }

    @Test
    void rejectsAnUnknownEntry() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId auditId = AggregateId.newId();
        when(repository.findById(tenantId, auditId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenantId, auditId))
                .isInstanceOf(AuditEntryNotFoundException.class);
    }

    @Test
    void rejectsANullAuditId() {
        assertThatThrownBy(() -> service.execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AuditApplicationMapper mapper = new AuditApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new GetAuditEntryApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetAuditEntryApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetAuditEntryApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
