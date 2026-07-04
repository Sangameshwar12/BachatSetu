package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.shared.domain.AggregateId;

@FunctionalInterface
public interface TenantScopeProvider {

    AggregateId currentTenantId();
}
