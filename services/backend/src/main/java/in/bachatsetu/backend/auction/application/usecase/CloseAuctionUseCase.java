package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.query.AuctionResult;

/** Closes an open auction with its winning member, making it immutable. */
@FunctionalInterface
public interface CloseAuctionUseCase {

    AuctionResult execute(CloseAuctionCommand command);
}
