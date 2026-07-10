package in.bachatsetu.backend.admin.application.usecase;

import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;

/** Lists tenants known to the platform (derived from their users), with per-tenant totals. */
@FunctionalInterface
public interface ListPlatformTenantsUseCase {

    PlatformPage<PlatformTenantResult> execute(PlatformPageRequest pageRequest);
}
