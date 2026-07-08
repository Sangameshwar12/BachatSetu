package in.bachatsetu.backend.auction.application.exception;

/** Base exception for application-level Auction failures. */
public class AuctionApplicationException extends RuntimeException {

    public AuctionApplicationException(String message) {
        super(message);
    }
}
