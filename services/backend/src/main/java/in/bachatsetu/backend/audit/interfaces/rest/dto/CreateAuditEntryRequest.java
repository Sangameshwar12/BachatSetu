package in.bachatsetu.backend.audit.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Manually records one audit entry through the REST boundary; tenant and actor come from the caller's identity. */
public record CreateAuditEntryRequest(

        @NotBlank
        @Schema(description = "Audit event type", example = "LOGIN")
        String eventType,

        @NotBlank
        @Schema(description = "Owning module name", example = "auth")
        String moduleName,

        @Schema(description = "Type of the resource this action concerns") String resourceType,
        @Schema(description = "Identifier of the resource this action concerns") String resourceId,

        @NotBlank
        @Schema(description = "Short action code", example = "LOGIN")
        String action,

        @Schema(description = "Human-readable description") String description,
        @Schema(description = "Caller IP address") String ipAddress,
        @Schema(description = "Caller user agent") String userAgent,
        @Schema(description = "Additional JSON metadata") String metadata) {
}
