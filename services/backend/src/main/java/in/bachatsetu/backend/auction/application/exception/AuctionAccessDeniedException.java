package in.bachatsetu.backend.auction.application.exception;

/** Raised when the acting user is not authorized to perform an auction operation. */
public final class AuctionAccessDeniedException extends AuctionApplicationException {

    public AuctionAccessDeniedException(String message) {
        super(message);
    }
}
