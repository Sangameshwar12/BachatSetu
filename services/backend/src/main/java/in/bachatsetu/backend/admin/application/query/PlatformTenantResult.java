package in.bachatsetu.backend.admin.application.query;

import java.util.Objects;
import java.util.UUID;

/** Application-layer read model for one tenant's totals. */
public record PlatformTenantResult(UUID tenantId, long userCount, long groupCount) {

    public PlatformTenantResult {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }
}
