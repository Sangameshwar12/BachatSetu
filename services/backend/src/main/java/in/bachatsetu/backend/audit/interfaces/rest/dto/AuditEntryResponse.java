package in.bachatsetu.backend.audit.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one audit entry returned by the REST API. */
public record AuditEntryResponse(

        @Schema(description = "Audit entry identifier") String auditId,
        @Schema(description = "Owning tenant identifier, absent for tenant-less events") String tenantId,
        @Schema(description = "Acting user identifier, absent for system events") String actorId,
        @Schema(description = "Audit event type", example = "LOGIN") String eventType,
        @Schema(description = "Owning module name", example = "auth") String moduleName,
        @Schema(description = "Type of the resource this action concerns") String resourceType,
        @Schema(description = "Identifier of the resource this action concerns") String resourceId,
        @Schema(description = "Short action code") String action,
        @Schema(description = "Human-readable description") String description,
        @Schema(description = "Caller IP address") String ipAddress,
        @Schema(description = "Caller user agent") String userAgent,
        @Schema(description = "Additional JSON metadata") String metadata,
        @Schema(description = "Timestamp the action occurred") Instant createdAt) {
}
