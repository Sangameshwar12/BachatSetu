package in.bachatsetu.backend.platformoperations.domain.model;

import java.time.Instant;

/**
 * Per-tenant totals, computed on demand from each existing module's own repository — no dedicated tenant
 * analytics table. {@code lastActivityAt} is a proxy: the most recently updated user record in the tenant,
 * since no cross-module "last activity" concept exists in this codebase.
 */
public record TenantStatistics(
        long users,
        long groups,
        long payments,
        long revenuePaise,
        long storageFiles,
        long storageBytes,
        long notifications,
        Instant lastActivityAt) {
}
