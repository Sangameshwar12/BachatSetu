package in.bachatsetu.backend.auction.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

/** Requests that an eligible member place a discount bid against an open auction. */
public record PlaceBidCommand(
        AggregateId tenantId,
        AggregateId auctionId,
        AggregateId memberId,
        Money discountAmount,
        AggregateId actorId) {

    public PlaceBidCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(discountAmount, "discount amount must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
