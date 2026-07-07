package in.bachatsetu.backend.draw.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Safe application view of one auction bid. */
public record AuctionBidResult(
        UUID bidId,
        UUID memberId,
        long discountAmountPaise,
        String currencyCode,
        Instant submittedAt,
        String status) {

    public AuctionBidResult {
        Objects.requireNonNull(bidId, "bid id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(submittedAt, "submitted at must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
