package in.bachatsetu.backend.audit.application.command;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Input for {@code CreateAuditEntryUseCase}. {@code tenantId} and {@code actorId} are both optional: an
 * authentication event has no tenant yet, and a system/background event has no human actor.
 */
public record CreateAuditEntryCommand(
        AggregateId tenantId,
        AggregateId actorId,
        AuditEventType eventType,
        String moduleName,
        String resourceType,
        AggregateId resourceId,
        String action,
        String description,
        String ipAddress,
        String userAgent,
        String metadata) {

    public CreateAuditEntryCommand {
        Objects.requireNonNull(eventType, "eventType must not be null");
        requireNonBlank(moduleName, "moduleName");
        requireNonBlank(action, "action");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
