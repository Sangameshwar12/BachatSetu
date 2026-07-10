package in.bachatsetu.backend.admin.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Read-only projection of one tenant, derived from the distinct tenant identifiers already recorded on
 * platform users — this codebase has no dedicated Tenant aggregate or table, so a tenant's existence is
 * inferred from having at least one user.
 */
public record PlatformTenantSummary(AggregateId tenantId, long userCount, long groupCount) {

    public PlatformTenantSummary {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        if (userCount < 0) {
            throw new IllegalArgumentException("userCount must not be negative");
        }
        if (groupCount < 0) {
            throw new IllegalArgumentException("groupCount must not be negative");
        }
    }
}
