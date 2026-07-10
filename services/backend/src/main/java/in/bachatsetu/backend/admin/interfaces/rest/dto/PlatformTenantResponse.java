package in.bachatsetu.backend.admin.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe presentation view of one tenant's totals returned by the REST API. */
public record PlatformTenantResponse(

        @Schema(description = "Tenant identifier") String tenantId,
        @Schema(description = "Number of users belonging to this tenant") long userCount,
        @Schema(description = "Number of savings groups belonging to this tenant") long groupCount) {
}
