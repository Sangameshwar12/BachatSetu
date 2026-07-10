package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

@FunctionalInterface
public interface GetTenantUseCase {

    TenantResult execute(AggregateId tenantId);
}
