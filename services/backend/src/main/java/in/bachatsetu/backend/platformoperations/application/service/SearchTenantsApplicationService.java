package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.SearchTenantsUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.domain.port.KnownTenantsRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.util.List;
import java.util.Objects;

/**
 * Enumerates tenants by reusing the same distinct-tenant-id derivation the Admin module already relies on
 * (a tenant "exists" by having at least one user — this module does not redesign that), then overlays each
 * tenant's own lifecycle status (if any platform-operations action was ever taken against it) and full
 * statistics. Uses {@link KnownTenantsRepository} rather than {@code admin.domain.port.PlatformTenantRepository}
 * directly, since Audit's REST layer already depends on this module (for the tenant lifecycle events) and a
 * dependency the other way, through Admin (which Audit also depends on), would form a module cycle.
 */
public final class SearchTenantsApplicationService implements SearchTenantsUseCase {

    private final KnownTenantsRepository knownTenantsRepository;
    private final TenantRepository tenantRepository;
    private final TenantStatisticsRepository statisticsRepository;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PlatformOperationsApplicationMapper mapper;

    public SearchTenantsApplicationService(
            KnownTenantsRepository knownTenantsRepository,
            TenantRepository tenantRepository,
            TenantStatisticsRepository statisticsRepository,
            ClockPort clock,
            TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        this.knownTenantsRepository =
                Objects.requireNonNull(knownTenantsRepository, "knownTenantsRepository must not be null");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Page<TenantResult> execute(TenantStatus statusFilter, PageQuery pageQuery) {
        Objects.requireNonNull(pageQuery, "pageQuery must not be null");
        return transaction.execute(() -> {
            Page<AggregateId> knownTenantIds = knownTenantsRepository.listKnownTenantIds(pageQuery);
            List<TenantResult> results = knownTenantIds.content().stream()
                    .map(this::toResult)
                    .filter(result -> statusFilter == null || result.status() == statusFilter)
                    .toList();
            return new Page<>(results, pageQuery.page(), pageQuery.size(), knownTenantIds.totalElements());
        });
    }

    private TenantResult toResult(AggregateId tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseGet(() -> Tenant.createActive(tenantId, tenantId, clock.now()));
        TenantStatistics statistics = statisticsRepository.computeFor(tenantId);
        return mapper.toResult(tenant, statistics);
    }
}
