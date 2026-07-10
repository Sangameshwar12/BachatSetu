package in.bachatsetu.backend.admin.application.analytics.query;

import java.util.UUID;

/** Application-layer read model mirroring {@code TenantUserCount}. */
public record TenantUserCountResult(UUID tenantId, long userCount) {
}
