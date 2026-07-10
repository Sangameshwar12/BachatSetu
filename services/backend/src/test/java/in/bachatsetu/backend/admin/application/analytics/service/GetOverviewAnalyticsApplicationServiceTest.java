package in.bachatsetu.backend.admin.application.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.mapper.AnalyticsApplicationMapper;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.OverviewAnalyticsRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GetOverviewAnalyticsApplicationServiceTest {

    private OverviewAnalyticsRepository repository;
    private CreateAuditEntryUseCase createAuditEntry;
    private GetOverviewAnalyticsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OverviewAnalyticsRepository.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new GetOverviewAnalyticsApplicationService(
                repository, new DirectTransactionPort(), new AnalyticsApplicationMapper(), createAuditEntry);
    }

    @Test
    void computesAndMapsOverviewThenRecordsAnAuditEntry() {
        when(repository.compute()).thenReturn(new OverviewAnalytics(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7));
        AggregateId administratorId = AggregateId.newId();

        OverviewAnalyticsResult result = service.execute(new ViewAnalyticsCommand(administratorId));

        assertThat(result.totalUsers()).isEqualTo(10);
        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        CreateAuditEntryCommand auditCommand = captor.getValue();
        assertThat(auditCommand.tenantId()).isNull();
        assertThat(auditCommand.actorId()).isEqualTo(administratorId);
        assertThat(auditCommand.eventType()).isEqualTo(AuditEventType.ADMIN_ANALYTICS_VIEWED);
        assertThat(auditCommand.moduleName()).isEqualTo("admin");
    }

    @Test
    void anAuditFailureDoesNotFailAnAlreadyComputedAnalyticsView() {
        when(repository.compute()).thenReturn(new OverviewAnalytics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        when(createAuditEntry.execute(any())).thenThrow(new RuntimeException("audit outage"));

        assertThatCode(() -> service.execute(new ViewAnalyticsCommand(AggregateId.newId())))
                .doesNotThrowAnyException();
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
