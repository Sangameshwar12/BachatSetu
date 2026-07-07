package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Draw aggregate. */
public final class GetDrawApplicationService implements GetDrawUseCase {

    private final DrawRepository repository;
    private final TransactionPort transaction;
    private final DrawApplicationMapper mapper;

    public GetDrawApplicationService(
            DrawRepository repository,
            TransactionPort transaction,
            DrawApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public DrawResult execute(AggregateId tenantId, AggregateId drawId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(drawId, "draw id must not be null");
        return transaction.execute(() -> {
            Draw draw = repository.findById(tenantId, drawId)
                    .orElseThrow(() -> new DrawNotFoundException("draw does not exist"));
            return mapper.toResult(draw);
        });
    }
}
