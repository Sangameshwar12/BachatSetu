package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateSystemLimitsCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateSystemLimitsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    private PlatformLimitRepository repository;
    private CreateAuditEntryUseCase createAuditEntry;
    private UpdateSystemLimitsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformLimitRepository.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new UpdateSystemLimitsApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper(), createAuditEntry,
                () -> NOW);
    }

    @Test
    void updatesALimitThenRecordsAnAuditEntry() {
        when(repository.findByKey(LimitKey.MAX_GROUPS))
                .thenReturn(Optional.of(PlatformLimit.of(LimitKey.MAX_GROUPS, 100, NOW)));
        when(repository.findAll()).thenReturn(List.of(PlatformLimit.of(LimitKey.MAX_GROUPS, 200, NOW)));
        AggregateId administratorId = AggregateId.newId();

        List<PlatformLimitResult> results = service.execute(new UpdateSystemLimitsCommand(
                administratorId, Map.of(LimitKey.MAX_GROUPS, 200L)));

        assertThat(results).hasSize(1);
        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SYSTEM_LIMIT_UPDATED);
    }

    @Test
    void createsAMissingLimitRowWhenNoneExistsYet() {
        when(repository.findByKey(LimitKey.MAX_UPLOADS)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of());

        service.execute(new UpdateSystemLimitsCommand(AggregateId.newId(), Map.of(LimitKey.MAX_UPLOADS, 50L)));

        verify(repository).save(any(PlatformLimit.class));
    }

    @Test
    void anAuditFailureDoesNotFailAnAlreadyAppliedUpdate() {
        when(repository.findByKey(any()))
                .thenReturn(Optional.of(PlatformLimit.of(LimitKey.MAX_RECEIPTS, 100, NOW)));
        when(repository.findAll()).thenReturn(List.of());
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("audit outage"));

        assertThatCode(() -> service.execute(new UpdateSystemLimitsCommand(
                AggregateId.newId(), Map.of(LimitKey.MAX_RECEIPTS, 300L)))).doesNotThrowAnyException();
    }

    @Test
    void rejectsANullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
