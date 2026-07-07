package in.bachatsetu.backend.draw.application.mapper;

import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.Draw;
import java.util.Objects;

/** Maps the Draw domain aggregate to immutable application query models. */
public final class DrawApplicationMapper {

    public DrawResult toResult(Draw draw) {
        Objects.requireNonNull(draw, "draw must not be null");
        return new DrawResult(
                draw.id().value(),
                draw.tenantId().value(),
                draw.groupId().value(),
                draw.cycleId().value(),
                draw.number().value(),
                draw.type().name(),
                draw.status().name(),
                draw.scheduledAt(),
                draw.winnerMemberId() == null ? null : draw.winnerMemberId().value(),
                draw.bids().stream().map(this::toBidResult).toList(),
                draw.auditInfo().createdAt(),
                draw.auditInfo().updatedAt(),
                draw.version());
    }

    public DrawSummary toSummary(Draw draw) {
        Objects.requireNonNull(draw, "draw must not be null");
        return new DrawSummary(
                draw.id().value(),
                draw.number().value(),
                draw.type().name(),
                draw.status().name(),
                draw.scheduledAt(),
                draw.winnerMemberId() == null ? null : draw.winnerMemberId().value());
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
}
