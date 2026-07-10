package in.bachatsetu.backend.admin.domain.analytics.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** One tenant's user count, for the platform-wide "users per tenant" distribution. */
public record TenantUserCount(AggregateId tenantId, long userCount) {

    public TenantUserCount {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        if (userCount < 0) {
            throw new IllegalArgumentException("userCount must not be negative");
        }
    }
}
