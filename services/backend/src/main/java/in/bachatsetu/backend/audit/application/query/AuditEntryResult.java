package in.bachatsetu.backend.audit.application.query;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Application-layer read model for one audit entry. */
public record AuditEntryResult(
        UUID auditId,
        UUID tenantId,
        UUID actorId,
        AuditEventType eventType,
        String moduleName,
        String resourceType,
        UUID resourceId,
        String action,
        String description,
        String ipAddress,
        String userAgent,
        String metadata,
        Instant createdAt) {

    public AuditEntryResult {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
