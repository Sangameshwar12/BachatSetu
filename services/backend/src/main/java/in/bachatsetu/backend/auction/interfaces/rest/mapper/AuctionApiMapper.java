package in.bachatsetu.backend.auction.interfaces.rest.mapper;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionBidResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionSummaryResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionWinnerResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CloseAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CreateAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PlaceBidRequest;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Auction application commands and safe responses. */
@Component
public class AuctionApiMapper {

    public CreateAuctionCommand toCreateCommand(CreateAuctionRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreateAuctionCommand(
                currentUser.tenantId(),
                AggregateId.from(request.groupId()),
                AggregateId.from(request.cycleId()),
                new DrawNumber(request.auctionNumber()),
                currentUser.userId().toAggregateId());
    }

    public PlaceBidCommand toPlaceBidCommand(
            String auctionId, PlaceBidRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        AggregateId memberId = currentUser.userId().toAggregateId();
        return new PlaceBidCommand(
                currentUser.tenantId(),
                AggregateId.from(auctionId),
                memberId,
                Money.inr(request.discountAmountPaise()),
                memberId);
    }

    public CloseAuctionCommand toCloseCommand(
            String auctionId, CloseAuctionRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CloseAuctionCommand(
                currentUser.tenantId(),
                AggregateId.from(auctionId),
                AggregateId.from(request.winnerId()),
                currentUser.userId().toAggregateId());
    }

    public AuctionResult getAuction(GetAuctionUseCase useCase, AuthenticatedUser currentUser, String auctionId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(auctionId, "auction id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(auctionId));
    }

    public AuctionWinnerResult getWinner(GetWinnerUseCase useCase, AuthenticatedUser currentUser, String auctionId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(auctionId, "auction id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(auctionId));
    }

    public AuctionResponse toResponse(AuctionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<AuctionBidResponse> bids = result.bids().stream().map(this::toBidResponse).toList();
        return new AuctionResponse(
                result.auctionId().toString(),
                result.tenantId().toString(),
                result.groupId().toString(),
                result.cycleId().toString(),
                result.number(),
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

    public AuctionWinnerResponse toWinnerResponse(AuctionWinnerResult winner) {
        Objects.requireNonNull(winner, "winner must not be null");
        return new AuctionWinnerResponse(
                winner.auctionId().toString(),
                winner.memberId().toString(),
                winner.winningDiscountAmountPaise(),
                winner.currencyCode(),
                winner.decidedAt());
    }

    public DrawPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new DrawPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public DrawPage<AuctionSummary> listAuctions(
            ListAuctionsUseCase useCase,
            AuthenticatedUser currentUser,
            DrawPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<AuctionSummaryResponse> listAuctions(
            ListAuctionsUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        DrawPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listAuctions(useCase, currentUser, pageRequest));
    }

    public AuctionSummaryResponse toSummaryResponse(AuctionSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new AuctionSummaryResponse(
                summary.auctionId().toString(),
                summary.number(),
                summary.status(),
                summary.scheduledAt(),
                summary.winnerMemberId() == null ? null : summary.winnerMemberId().toString());
    }

    public PageResponse<AuctionSummaryResponse> toSummaryPage(DrawPage<AuctionSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<AuctionSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
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
