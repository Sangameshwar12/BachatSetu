package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.domain.port.KnownTenantsRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SearchTenantsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final KnownTenantsRepository knownTenantsRepository = mock(KnownTenantsRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantStatisticsRepository statisticsRepository = mock(TenantStatisticsRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final SearchTenantsApplicationService service = new SearchTenantsApplicationService(
            knownTenantsRepository, tenantRepository, statisticsRepository, clock, transaction, mapper);

    @Test
    void enumeratesKnownTenantsViaTheKnownTenantsRepository() {
        AggregateId tenantId = AggregateId.newId();
        when(knownTenantsRepository.listKnownTenantIds(new PageQuery(0, 20)))
                .thenReturn(new Page<>(List.of(tenantId), 0, 20, 1));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(NOW);
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(1, 1, 1, 0, 0, 0, 1, NOW));

        Page<TenantResult> result = service.execute(null, new PageQuery(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void filtersByStatusAfterOverlayingLifecycleState() {
        AggregateId tenantId = AggregateId.newId();
        when(knownTenantsRepository.listKnownTenantIds(new PageQuery(0, 20)))
                .thenReturn(new Page<>(List.of(tenantId), 0, 20, 1));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(NOW);
        when(statisticsRepository.computeFor(tenantId)).thenReturn(
                new TenantStatistics(0, 0, 0, 0, 0, 0, 0, null));

        Page<TenantResult> result = service.execute(TenantStatus.SUSPENDED, new PageQuery(0, 20));

        assertThat(result.content()).isEmpty();
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
