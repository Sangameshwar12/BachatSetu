package in.bachatsetu.backend.draw.domain.model;

import in.bachatsetu.backend.draw.domain.exception.InvalidBidStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public final class AuctionBid {

    private final AggregateId id;
    private final AggregateId memberId;
    private final BidAmount amount;
    private final Instant submittedAt;
    private BidStatus status;

    public AuctionBid(
            AggregateId id,
            AggregateId memberId,
            BidAmount amount,
            Instant submittedAt,
            BidStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void markLeading() {
        if (status != BidStatus.SUBMITTED && status != BidStatus.OUTBID) {
            throw new InvalidBidStateException("bid cannot become leading");
        }
        status = BidStatus.LEADING;
    }

    public void markOutbid() {
        if (status != BidStatus.LEADING && status != BidStatus.SUBMITTED) {
            throw new InvalidBidStateException("bid cannot be marked outbid");
        }
        status = BidStatus.OUTBID;
    }

    public void accept() {
        if (status != BidStatus.LEADING) {
            throw new InvalidBidStateException("only the leading bid can be accepted");
        }
        status = BidStatus.ACCEPTED;
    }

    public AggregateId id() { return id; }
    public AggregateId memberId() { return memberId; }
    public BidAmount amount() { return amount; }
    public Instant submittedAt() { return submittedAt; }
    public BidStatus status() { return status; }
}
