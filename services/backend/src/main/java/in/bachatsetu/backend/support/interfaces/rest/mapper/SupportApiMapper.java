package in.bachatsetu.backend.support.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.SortDirection;
import in.bachatsetu.backend.support.application.command.AssignTicketCommand;
import in.bachatsetu.backend.support.application.command.CloseTicketCommand;
import in.bachatsetu.backend.support.application.command.CreateTicketCommand;
import in.bachatsetu.backend.support.application.command.ResolveTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;
import in.bachatsetu.backend.support.interfaces.rest.dto.AssignTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.CreateTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.ResolveTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketPageResponse;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketResponse;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SupportApiMapper {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    public CreateTicketCommand toCreateCommand(AuthenticatedUser currentUser, CreateTicketRequest request) {
        return new CreateTicketCommand(
                currentUser.tenantId(), currentUser.userId().toAggregateId(),
                TicketCategory.valueOf(request.category()), TicketPriority.valueOf(request.priority()),
                request.subject(), request.description());
    }

    public AssignTicketCommand toAssignCommand(String ticketId, AuthenticatedUser currentUser, AssignTicketRequest request) {
        return new AssignTicketCommand(
                AggregateId.from(ticketId), AggregateId.from(request.assigneeId()),
                currentUser.userId().toAggregateId());
    }

    public ResolveTicketCommand toResolveCommand(
            String ticketId, AuthenticatedUser currentUser, ResolveTicketRequest request) {
        return new ResolveTicketCommand(
                AggregateId.from(ticketId), request.resolution(), currentUser.userId().toAggregateId());
    }

    public CloseTicketCommand toCloseCommand(String ticketId, AuthenticatedUser currentUser) {
        return new CloseTicketCommand(AggregateId.from(ticketId), currentUser.userId().toAggregateId());
    }

    public TicketResponse get(GetTicketUseCase useCase, String ticketId) {
        return toResponse(useCase.execute(AggregateId.from(ticketId)));
    }

    public SupportTicketSearchCriteria toSearchCriteria(
            String status, String priority, String category, String tenantId, String raisedBy,
            Instant createdAfter, Instant createdBefore, Integer page, Integer size, String direction) {
        return new SupportTicketSearchCriteria(
                status == null ? null : TicketStatus.valueOf(status),
                priority == null ? null : TicketPriority.valueOf(priority),
                category == null ? null : TicketCategory.valueOf(category),
                tenantId == null ? null : AggregateId.from(tenantId),
                raisedBy == null ? null : AggregateId.from(raisedBy),
                createdAfter, createdBefore,
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                "asc".equalsIgnoreCase(direction) ? SortDirection.ASC : SortDirection.DESC);
    }

    public TicketResponse toResponse(SupportTicketResult result) {
        return new TicketResponse(
                result.ticketId().toString(), result.tenantId().toString(), result.raisedBy().toString(),
                result.category().name(), result.priority().name(), result.status().name(), result.subject(),
                result.description(), result.assignedTo() == null ? null : result.assignedTo().toString(),
                result.createdAt(), result.resolvedAt(), result.resolution());
    }

    public TicketPageResponse<TicketResponse> toPageResponse(Page<SupportTicketResult> page) {
        return new TicketPageResponse<>(
                page.content().stream().map(this::toResponse).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages(), page.hasNext(), page.hasPrevious());
    }
}
