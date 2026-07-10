package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;

/**
 * Enumerates every tenant known to the platform — a tenant "exists" by having at least one user, mirroring
 * (independently of, to avoid a module dependency cycle with Audit) {@code
 * admin.domain.port.PlatformTenantRepository}'s derivation.
 */
public interface KnownTenantsRepository {

    Page<AggregateId> listKnownTenantIds(PageQuery pageQuery);
}
