package in.bachatsetu.backend.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.application.port.ClockPort;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateAuditEntryApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private AuditRepository repository;
    private AuditPublisherPort publisher;
    private CreateAuditEntryApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditRepository.class);
        publisher = mock(AuditPublisherPort.class);
        ClockPort clock = () -> NOW;
        service = new CreateAuditEntryApplicationService(
                repository, publisher, clock, new DirectTransactionPort(), new AuditApplicationMapper());
    }

    @Test
    void recordsSavesPublishesAndMapsAnEntry() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        CreateAuditEntryCommand command = new CreateAuditEntryCommand(
                tenantId, actorId, AuditEventType.PAYMENT_VERIFIED, "payment", "Payment", AggregateId.newId(),
                "PAYMENT_VERIFIED", "payment verified", null, null, null);

        AuditEntryResult result = service.execute(command);

        assertThat(result.tenantId()).isEqualTo(tenantId.value());
        assertThat(result.actorId()).isEqualTo(actorId.value());
        assertThat(result.eventType()).isEqualTo(AuditEventType.PAYMENT_VERIFIED);
        assertThat(result.createdAt()).isEqualTo(NOW);
        verify(repository).save(any(AuditEntry.class));
        verify(publisher).publish(any(AuditEntry.class));
    }

    @Test
    void recordsAnEntryWithNoTenantOrActor() {
        CreateAuditEntryCommand command = new CreateAuditEntryCommand(
                null, null, AuditEventType.SYSTEM_EVENT, "automation", null, null, "SYSTEM_EVENT", null, null,
                null, null);

        AuditEntryResult result = service.execute(command);

        assertThat(result.tenantId()).isNull();
        assertThat(result.actorId()).isNull();
    }

    @Test
    void rejectsANullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AuditApplicationMapper mapper = new AuditApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        ClockPort clock = () -> NOW;
        assertThatThrownBy(() -> new CreateAuditEntryApplicationService(null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuditEntryApplicationService(repository, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuditEntryApplicationService(repository, publisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuditEntryApplicationService(repository, publisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuditEntryApplicationService(repository, publisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
