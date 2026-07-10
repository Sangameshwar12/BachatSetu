package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Computes per-tenant totals on demand, composing every existing module's own repository. */
public interface TenantStatisticsRepository {

    TenantStatistics computeFor(AggregateId tenantId);
}
