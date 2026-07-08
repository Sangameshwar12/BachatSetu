package in.bachatsetu.backend.audit.interfaces.rest.mapper;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.AuditSortField;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.audit.interfaces.rest.dto.AuditEntryResponse;
import in.bachatsetu.backend.audit.interfaces.rest.dto.CreateAuditEntryRequest;
import in.bachatsetu.backend.audit.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Audit application commands and safe responses. */
@Component
public class AuditApiMapper {

    public CreateAuditEntryCommand toCreateCommand(
            CreateAuditEntryRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreateAuditEntryCommand(
                currentUser.tenantId(),
                currentUser.userId().toAggregateId(),
                AuditEventType.valueOf(request.eventType()),
                request.moduleName(),
                request.resourceType(),
                request.resourceId() == null ? null : AggregateId.from(request.resourceId()),
                request.action(),
                request.description(),
                request.ipAddress(),
                request.userAgent(),
                request.metadata());
    }

    public AggregateId toAuditId(String auditId) {
        Objects.requireNonNull(auditId, "audit id must not be null");
        return AggregateId.from(auditId);
    }

    public AuditSearchCriteria toSearchCriteria(
            AuthenticatedUser currentUser,
            String actor,
            String module,
            String event,
            Instant dateFrom,
            Instant dateTo,
            int page,
            int size,
            String sort,
            String direction) {
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new AuditSearchCriteria(
                currentUser.tenantId(),
                actor == null ? null : AggregateId.from(actor),
                module,
                event == null ? null : AuditEventType.valueOf(event),
                dateFrom,
                dateTo,
                page,
                size,
                toSortField(sort),
                toSortDirection(direction));
    }

    public AuditEntryResponse toResponse(AuditEntryResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new AuditEntryResponse(
                result.auditId().toString(),
                result.tenantId() == null ? null : result.tenantId().toString(),
                result.actorId() == null ? null : result.actorId().toString(),
                result.eventType().name(),
                result.moduleName(),
                result.resourceType(),
                result.resourceId() == null ? null : result.resourceId().toString(),
                result.action(),
                result.description(),
                result.ipAddress(),
                result.userAgent(),
                result.metadata(),
                result.createdAt());
    }

    public PageResponse<AuditEntryResponse> toPageResponse(AuditPage<AuditEntryResult> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<AuditEntryResponse> content = page.content().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext(),
                page.hasPrevious());
    }

    private AuditSortField toSortField(String sort) {
        return switch (sort) {
            case "createdAt" -> AuditSortField.CREATED_AT;
            default -> throw new IllegalArgumentException("unsupported sort field: " + sort);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("unsupported sort direction: " + direction);
        };
    }
}
