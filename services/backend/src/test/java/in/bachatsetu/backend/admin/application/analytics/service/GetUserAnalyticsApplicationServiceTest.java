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
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.UserAnalyticsRepository;
import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GetUserAnalyticsApplicationServiceTest {

    private UserAnalyticsRepository repository;
    private CreateAuditEntryUseCase createAuditEntry;
    private GetUserAnalyticsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserAnalyticsRepository.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        service = new GetUserAnalyticsApplicationService(
                repository, new DirectTransactionPort(), new AnalyticsApplicationMapper(), createAuditEntry);
    }

    @Test
    void computesAndMapsUserAnalyticsThenRecordsAnAuditEntry() {
        when(repository.compute()).thenReturn(new UserAnalytics(10, 8, 2, List.of(), List.of(), List.of()));
        AggregateId administratorId = AggregateId.newId();

        UserAnalyticsResult result = service.execute(new ViewAnalyticsCommand(administratorId));

        assertThat(result.totalUsers()).isEqualTo(10);
        ArgumentCaptor<CreateAuditEntryCommand> captor = ArgumentCaptor.forClass(CreateAuditEntryCommand.class);
        verify(createAuditEntry).execute(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.ADMIN_ANALYTICS_VIEWED);
    }

    @Test
    void anAuditFailureDoesNotFailAnAlreadyComputedAnalyticsView() {
        when(repository.compute()).thenReturn(new UserAnalytics(0, 0, 0, List.of(), List.of(), List.of()));
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
