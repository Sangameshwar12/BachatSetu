package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves the winning member and accepted bid of a closed, tenant-scoped auction. */
@FunctionalInterface
public interface GetWinnerUseCase {

    AuctionWinnerResult execute(AggregateId tenantId, AggregateId auctionId);
}
