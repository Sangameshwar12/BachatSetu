package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * A tenant with no persisted lifecycle record (never suspended, activated, or archived) is reported as
 * {@link in.bachatsetu.backend.platformoperations.domain.model.TenantStatus#ACTIVE} by default, matching
 * {@link Tenant}'s lazily-created-on-first-action design.
 */
public final class GetTenantApplicationService implements GetTenantUseCase {

    private final TenantRepository tenantRepository;
    private final TenantStatisticsRepository statisticsRepository;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public GetTenantApplicationService(
            TenantRepository tenantRepository,
            TenantStatisticsRepository statisticsRepository,
            ClockPort clock,
            TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public TenantResult execute(AggregateId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return transaction.execute(() -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseGet(() -> Tenant.createActive(tenantId, tenantId, clock.now()));
            TenantStatistics statistics = statisticsRepository.computeFor(tenantId);
            return mapper.toResult(tenant, statistics);
        });
    }
}
