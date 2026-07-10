package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.platformoperations.application.command.SuspendTenantCommand;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SuspendTenantApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantStatisticsRepository statisticsRepository = mock(TenantStatisticsRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final SuspendTenantApplicationService service = new SuspendTenantApplicationService(
            tenantRepository, statisticsRepository, eventPublisher, createAuditEntry, clock, transaction, mapper);

    @Test
    void lazilyCreatesAndSuspendsATenantWithNoPriorRecord() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId admin = AggregateId.newId();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(NOW);
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(1, 1, 1, 100, 0, 0, 1, NOW));

        TenantResult result = service.execute(new SuspendTenantCommand(tenantId, "Fraud", admin));

        assertThat(result.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(result.suspensionReason()).isEqualTo("Fraud");
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
        verify(createAuditEntry).execute(any());
    }

    @Test
    void neverLetsAnAuditFailureAffectTheSuspension() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId admin = AggregateId.newId();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(NOW);
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(0, 0, 0, 0, 0, 0, 0, null));
        org.mockito.Mockito.doThrow(new RuntimeException("audit down")).when(createAuditEntry).execute(any());

        TenantResult result = service.execute(new SuspendTenantCommand(tenantId, "Reason", admin));

        assertThat(result.status()).isEqualTo(TenantStatus.SUSPENDED);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
