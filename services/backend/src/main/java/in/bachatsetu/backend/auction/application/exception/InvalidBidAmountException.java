package in.bachatsetu.backend.auction.application.exception;

/** Raised when a bid's discount amount falls outside the group's permitted range. */
public final class InvalidBidAmountException extends AuctionApplicationException {

    public InvalidBidAmountException(String message) {
        super(message);
    }
}
