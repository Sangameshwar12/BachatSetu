package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
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

class GetTenantApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantStatisticsRepository statisticsRepository = mock(TenantStatisticsRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final GetTenantApplicationService service =
            new GetTenantApplicationService(tenantRepository, statisticsRepository, clock, transaction, mapper);

    @Test
    void returnsAPersistedTenant() {
        AggregateId tenantId = AggregateId.newId();
        Tenant tenant = Tenant.createActive(tenantId, AggregateId.newId(), NOW);
        tenant.suspend("Reason", AggregateId.newId(), NOW);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(1, 1, 1, 100, 1, 10, 1, NOW));

        TenantResult result = service.execute(tenantId);

        assertThat(result.status()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    void defaultsToActiveForATenantWithNoPriorRecord() {
        AggregateId tenantId = AggregateId.newId();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(NOW);
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(0, 0, 0, 0, 0, 0, 0, null));

        TenantResult result = service.execute(tenantId);

        assertThat(result.status()).isEqualTo(TenantStatus.ACTIVE);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
