package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;

/** Places an eligible member's discount bid against an open auction. */
@FunctionalInterface
public interface PlaceBidUseCase {

    AuctionBidResult execute(PlaceBidCommand command);
}
