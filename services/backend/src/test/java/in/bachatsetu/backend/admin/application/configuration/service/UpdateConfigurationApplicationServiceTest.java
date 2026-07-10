package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateConfigurationCommand;
import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateConfigurationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    private PlatformConfigurationRepository repository;
    private CreateAuditEntryUseCase createAuditEntry;
    private UpdateConfigurationApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformConfigurationRepository.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new UpdateConfigurationApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper(), createAuditEntry,
                () -> NOW);
    }

    @Test
    void updatesTheConfigurationThenRecordsAnAuditEntry() {
        when(repository.find()).thenReturn(PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null));
        AggregateId administratorId = AggregateId.newId();

        PlatformConfigurationResult result = service.execute(new UpdateConfigurationCommand(
                administratorId, "HINDI", 600, "AWS_S3", "STRIPE", 5, 20_000_000L, 200, 40, false, null, null,
                null));

        assertThat(result.defaultLanguage()).isEqualTo("HINDI");
        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.PLATFORM_CONFIGURATION_UPDATED);
    }

    @Test
    void anAuditFailureDoesNotFailAnAlreadyAppliedUpdate() {
        when(repository.find()).thenReturn(PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null));
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("audit outage"));

        assertThatCode(() -> service.execute(new UpdateConfigurationCommand(
                AggregateId.newId(), "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null,
                null, null))).doesNotThrowAnyException();
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
