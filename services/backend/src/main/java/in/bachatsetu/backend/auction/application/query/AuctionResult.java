package in.bachatsetu.backend.auction.application.query;

import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of an auction (an auction-type Draw aggregate). */
public record AuctionResult(
        UUID auctionId,
        UUID tenantId,
        UUID groupId,
        UUID cycleId,
        int number,
        String status,
        Instant scheduledAt,
        UUID winnerMemberId,
        List<AuctionBidResult> bids,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public AuctionResult {
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(cycleId, "cycle id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
        bids = List.copyOf(Objects.requireNonNull(bids, "bids must not be null"));
        Objects.requireNonNull(createdAt, "created at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
    }
}
