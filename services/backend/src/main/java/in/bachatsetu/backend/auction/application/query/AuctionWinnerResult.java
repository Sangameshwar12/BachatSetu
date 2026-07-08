package in.bachatsetu.backend.auction.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The winning member and accepted bid of a closed auction. */
public record AuctionWinnerResult(
        UUID auctionId,
        UUID memberId,
        long winningDiscountAmountPaise,
        String currencyCode,
        Instant decidedAt) {

    public AuctionWinnerResult {
        Objects.requireNonNull(auctionId, "auction id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(decidedAt, "decided at must not be null");
    }
}
