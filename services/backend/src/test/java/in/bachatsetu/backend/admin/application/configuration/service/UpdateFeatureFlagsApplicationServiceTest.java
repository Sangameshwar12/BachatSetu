package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateFeatureFlagsCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
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

class UpdateFeatureFlagsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    private FeatureFlagRepository repository;
    private CreateAuditEntryUseCase createAuditEntry;
    private UpdateFeatureFlagsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(FeatureFlagRepository.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new UpdateFeatureFlagsApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper(), createAuditEntry,
                () -> NOW);
    }

    @Test
    void disablesAFlagThenRecordsAnAuditEntry() {
        when(repository.findByKey(FeatureKey.PAYMENTS))
                .thenReturn(Optional.of(FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW)));
        when(repository.findAll()).thenReturn(List.of(FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW)
                .withEnabled(false, AggregateId.newId(), NOW)));
        AggregateId administratorId = AggregateId.newId();

        List<FeatureFlagResult> results = service.execute(new UpdateFeatureFlagsCommand(
                administratorId, Map.of(FeatureKey.PAYMENTS, false)));

        assertThat(results).hasSize(1);
        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.FEATURE_FLAG_UPDATED);
    }

    @Test
    void createsAMissingFlagRowWhenNoneExistsYet() {
        when(repository.findByKey(FeatureKey.SIGNUP)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of());

        service.execute(new UpdateFeatureFlagsCommand(AggregateId.newId(), Map.of(FeatureKey.SIGNUP, false)));

        verify(repository).save(any(FeatureFlag.class));
    }

    @Test
    void anAuditFailureDoesNotFailAnAlreadyAppliedUpdate() {
        when(repository.findByKey(any())).thenReturn(Optional.of(FeatureFlag.defaultEnabled(FeatureKey.STORAGE, NOW)));
        when(repository.findAll()).thenReturn(List.of());
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("audit outage"));

        assertThatCode(() -> service.execute(new UpdateFeatureFlagsCommand(
                AggregateId.newId(), Map.of(FeatureKey.STORAGE, false)))).doesNotThrowAnyException();
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
