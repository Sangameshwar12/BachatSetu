package in.bachatsetu.backend.platformoperations.application.query;

import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;

public record TenantResult(
        AggregateId tenantId, TenantStatus status, String suspensionReason, TenantStatistics statistics) {
}
