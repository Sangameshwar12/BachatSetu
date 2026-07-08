package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped auction. */
@FunctionalInterface
public interface GetAuctionUseCase {

    AuctionResult execute(AggregateId tenantId, AggregateId auctionId);
}
