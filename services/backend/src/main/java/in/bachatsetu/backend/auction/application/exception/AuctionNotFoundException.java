package in.bachatsetu.backend.auction.application.exception;

/** Raised when a tenant-scoped auction lookup has no result. */
public final class AuctionNotFoundException extends AuctionApplicationException {

    public AuctionNotFoundException(String message) {
        super(message);
    }
}
