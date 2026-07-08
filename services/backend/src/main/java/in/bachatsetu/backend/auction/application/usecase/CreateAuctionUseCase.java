package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.query.AuctionResult;

/** Schedules a new auction, opened immediately so it can accept bids. */
@FunctionalInterface
public interface CreateAuctionUseCase {

    AuctionResult execute(CreateAuctionCommand command);
}
