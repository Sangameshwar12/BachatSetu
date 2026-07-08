package in.bachatsetu.backend.auction.application.usecase;

import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact auction views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListAuctionsUseCase {

    DrawPage<AuctionSummary> execute(AggregateId tenantId, DrawPageRequest pageRequest);
}
