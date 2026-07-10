package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;

/** Cross-tenant read access to tenant-level totals, derived from users and groups. */
public interface PlatformTenantRepository {

    PlatformPage<PlatformTenantSummary> search(PlatformPageRequest pageRequest);
}
