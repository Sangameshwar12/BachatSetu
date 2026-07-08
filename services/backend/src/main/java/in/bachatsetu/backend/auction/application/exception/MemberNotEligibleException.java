package in.bachatsetu.backend.auction.application.exception;

/** Raised when a bidding member is not an active participant of the auction's savings group. */
public final class MemberNotEligibleException extends AuctionApplicationException {

    public MemberNotEligibleException(String message) {
        super(message);
    }
}
