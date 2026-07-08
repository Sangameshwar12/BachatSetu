package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves the winning member and accepted bid of a closed, tenant-scoped auction. */
public final class GetWinnerApplicationService implements GetWinnerUseCase {

    private final DrawRepository repository;
    private final TransactionPort transaction;
    private final AuctionApplicationMapper mapper;

    public GetWinnerApplicationService(
            DrawRepository repository,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AuctionWinnerResult execute(AggregateId tenantId, AggregateId auctionId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(auctionId, "auction id must not be null");
        return transaction.execute(() -> {
            Draw auction = repository.findById(tenantId, auctionId)
                    .filter(draw -> draw.type() == DrawType.AUCTION)
                    .orElseThrow(() -> new AuctionNotFoundException("auction does not exist"));
            return mapper.toWinnerResult(auction);
        });
    }
}
