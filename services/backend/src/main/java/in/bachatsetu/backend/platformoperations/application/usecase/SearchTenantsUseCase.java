package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;

@FunctionalInterface
public interface SearchTenantsUseCase {

    Page<TenantResult> execute(TenantStatus statusFilter, PageQuery pageQuery);
}
