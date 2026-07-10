package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** One tenant's user count. */
public record TenantUserCountResponse(
        @Schema(description = "Tenant identifier") String tenantId,
        @Schema(description = "Number of users belonging to this tenant") long userCount) {
}
