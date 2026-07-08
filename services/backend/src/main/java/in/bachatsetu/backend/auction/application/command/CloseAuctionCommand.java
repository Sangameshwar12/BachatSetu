package in.bachatsetu.backend.auction.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that an open auction be closed with its winning member. */
public record CloseAuctionCommand(AggregateId tenantId, AggregateId auctionId, AggregateId winnerId, AggregateId actorId) {

    public CloseAuctionCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(winnerId, "winner id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
