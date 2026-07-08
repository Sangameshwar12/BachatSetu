package in.bachatsetu.backend.auction.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Compact auction view optimized for list use cases. */
public record AuctionSummary(
        UUID auctionId,
        int number,
        String status,
        Instant scheduledAt,
        UUID winnerMemberId) {

    public AuctionSummary {
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
    }
}
