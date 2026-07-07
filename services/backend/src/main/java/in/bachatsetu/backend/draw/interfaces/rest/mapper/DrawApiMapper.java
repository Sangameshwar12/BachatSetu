package in.bachatsetu.backend.draw.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.draw.interfaces.rest.dto.AuctionBidResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CloseDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CreateDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawSummaryResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Draw application commands and safe responses. */
@Component
public class DrawApiMapper {

    public CreateDrawCommand toCreateCommand(CreateDrawRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreateDrawCommand(
                currentUser.tenantId(),
                AggregateId.from(request.groupId()),
                AggregateId.from(request.cycleId()),
                new DrawNumber(request.drawNumber()),
                DrawType.valueOf(request.type()),
                request.scheduledAt(),
                currentUser.userId().toAggregateId());
    }

    public DrawResult getDraw(GetDrawUseCase useCase, AuthenticatedUser currentUser, String drawId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(drawId, "draw id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(drawId));
    }

    public DrawResponse toResponse(DrawResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<AuctionBidResponse> bids = result.bids().stream().map(this::toBidResponse).toList();
        return new DrawResponse(
                result.drawId().toString(),
                result.tenantId().toString(),
                result.groupId().toString(),
                result.cycleId().toString(),
                result.number(),
                result.type(),
                result.status(),
                result.scheduledAt(),
                result.winnerMemberId() == null ? null : result.winnerMemberId().toString(),
                bids,
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }

    public AuctionBidResponse toBidResponse(AuctionBidResult bid) {
        Objects.requireNonNull(bid, "bid must not be null");
        return new AuctionBidResponse(
                bid.bidId().toString(),
                bid.memberId().toString(),
                bid.discountAmountPaise(),
                bid.currencyCode(),
                bid.submittedAt(),
                bid.status());
    }

    public DrawPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new DrawPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public DrawPage<DrawSummary> listDraws(
            ListDrawsUseCase useCase,
            AuthenticatedUser currentUser,
            DrawPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<DrawSummaryResponse> listDraws(
            ListDrawsUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        DrawPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listDraws(useCase, currentUser, pageRequest));
    }

    public DrawSummaryResponse toSummaryResponse(DrawSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new DrawSummaryResponse(
                summary.drawId().toString(),
                summary.number(),
                summary.type(),
                summary.status(),
                summary.scheduledAt(),
                summary.winnerMemberId() == null ? null : summary.winnerMemberId().toString());
    }

    public PageResponse<DrawSummaryResponse> toSummaryPage(DrawPage<DrawSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<DrawSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    public ConductDrawCommand toConductCommand(String drawId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new ConductDrawCommand(
                currentUser.tenantId(), AggregateId.from(drawId), currentUser.userId().toAggregateId());
    }

    public CloseDrawCommand toCloseCommand(String drawId, CloseDrawRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CloseDrawCommand(
                currentUser.tenantId(),
                AggregateId.from(drawId),
                AggregateId.from(request.winnerId()),
                currentUser.userId().toAggregateId());
    }

    private DrawSortField toSortField(String sort) {
        return switch (sort) {
            case "scheduledAt" -> DrawSortField.SCHEDULED_AT;
            case "createdAt" -> DrawSortField.CREATED_AT;
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
