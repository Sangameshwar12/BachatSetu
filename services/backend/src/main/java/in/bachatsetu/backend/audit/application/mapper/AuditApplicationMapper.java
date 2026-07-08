package in.bachatsetu.backend.audit.application.mapper;

import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;

/** Converts the {@link AuditEntry} aggregate to application-layer read models. */
public final class AuditApplicationMapper {

    public AuditEntryResult toResult(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        return new AuditEntryResult(
                entry.id().value(),
                value(entry.tenantId()),
                value(entry.actorId()),
                entry.eventType(),
                entry.moduleName(),
                entry.resourceType(),
                value(entry.resourceId()),
                entry.action(),
                entry.description(),
                entry.ipAddress(),
                entry.userAgent(),
                entry.metadata(),
                entry.createdAt());
    }

    public AuditPage<AuditEntryResult> toResultPage(AuditPage<AuditEntry> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new AuditPage<>(
                page.content().stream().map(this::toResult).toList(), page.page(), page.size(),
                page.totalElements());
    }

    private static UUID value(AggregateId id) {
        return id == null ? null : id.value();
    }
}
