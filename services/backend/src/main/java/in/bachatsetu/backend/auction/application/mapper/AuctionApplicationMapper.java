package in.bachatsetu.backend.auction.application.mapper;

import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.AuctionWinner;
import in.bachatsetu.backend.draw.domain.model.Draw;
import java.util.Objects;

/** Maps the Draw domain aggregate to immutable Auction application query models. */
public final class AuctionApplicationMapper {

    public AuctionResult toResult(Draw auction) {
        Objects.requireNonNull(auction, "auction must not be null");
        return new AuctionResult(
                auction.id().value(),
                auction.tenantId().value(),
                auction.groupId().value(),
                auction.cycleId().value(),
                auction.number().value(),
                auction.status().name(),
                auction.scheduledAt(),
                auction.winnerMemberId() == null ? null : auction.winnerMemberId().value(),
                auction.bids().stream().map(this::toBidResult).toList(),
                auction.auditInfo().createdAt(),
                auction.auditInfo().updatedAt(),
                auction.version());
    }

    public AuctionSummary toSummary(Draw auction) {
        Objects.requireNonNull(auction, "auction must not be null");
        return new AuctionSummary(
                auction.id().value(),
                auction.number().value(),
                auction.status().name(),
                auction.scheduledAt(),
                auction.winnerMemberId() == null ? null : auction.winnerMemberId().value());
    }

    public AuctionBidResult toBidResult(AuctionBid bid) {
        Objects.requireNonNull(bid, "bid must not be null");
        return new AuctionBidResult(
                bid.id().value(),
                bid.memberId().value(),
                bid.amount().discount().minorUnits(),
                bid.amount().discount().currency().getCurrencyCode(),
                bid.submittedAt(),
                bid.status().name());
    }

    public AuctionWinnerResult toWinnerResult(Draw auction) {
        Objects.requireNonNull(auction, "auction must not be null");
        AuctionWinner winner = auction.winner()
                .orElseThrow(() -> new AuctionNotFoundException("auction has no recorded winner"));
        return new AuctionWinnerResult(
                auction.id().value(),
                winner.memberId().value(),
                winner.winningAmount().discount().minorUnits(),
                winner.winningAmount().discount().currency().getCurrencyCode(),
                winner.decidedAt());
    }
}
