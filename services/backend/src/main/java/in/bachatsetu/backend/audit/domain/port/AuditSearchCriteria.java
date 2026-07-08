package in.bachatsetu.backend.audit.domain.port;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Tenant-scoped search request carrying every optional filter, plus pagination and sorting.
 *
 * <p>{@code tenantId} is nullable so this port can, in principle, express a search across tenant-less
 * (system) entries too — but the REST boundary always supplies the caller's own tenant, so an authenticated
 * caller never sees another tenant's entries. Every other filter is optional: a {@code null} value means
 * "do not filter on this field."
 */
public record AuditSearchCriteria(
        AggregateId tenantId,
        AggregateId actorId,
        String moduleName,
        AuditEventType eventType,
        Instant dateFrom,
        Instant dateTo,
        int page,
        int size,
        AuditSortField sortField,
        SortDirection direction) {

    public static final int MAXIMUM_SIZE = 100;

    public AuditSearchCriteria {
        Objects.requireNonNull(sortField, "sort field must not be null");
        Objects.requireNonNull(direction, "sort direction must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_SIZE);
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }
    }
}
