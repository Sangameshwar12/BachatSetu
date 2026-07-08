package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/**
 * Lists tenant-scoped auctions as compact immutable query models, paginated by the repository.
 *
 * <p>Calls {@link DrawRepository#findPageByType} (an additive method this sprint introduced) rather than
 * the pre-existing {@code findPage}, so that pagination totals reflect only auction-type draws — filtering
 * a {@code findPage} result in memory after the fact would silently corrupt {@code totalElements} and
 * {@code hasNext}/{@code hasPrevious} for tenants that also have RANDOM or FIXED_ROTATION draws.
 */
public final class ListAuctionsApplicationService implements ListAuctionsUseCase {

    private final DrawRepository repository;
    private final TransactionPort transaction;
    private final AuctionApplicationMapper mapper;

    public ListAuctionsApplicationService(
            DrawRepository repository,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public DrawPage<AuctionSummary> execute(AggregateId tenantId, DrawPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            DrawPage<Draw> page = repository.findPageByType(tenantId, DrawType.AUCTION, pageRequest);
            List<AuctionSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new DrawPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
