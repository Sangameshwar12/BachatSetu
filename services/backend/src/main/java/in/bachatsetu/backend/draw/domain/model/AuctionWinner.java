package in.bachatsetu.backend.draw.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** The accepted winning bid of a completed auction-type draw. */
public record AuctionWinner(AggregateId memberId, BidAmount winningAmount, Instant decidedAt) {

    public AuctionWinner {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(winningAmount, "winningAmount must not be null");
        Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    }
}
