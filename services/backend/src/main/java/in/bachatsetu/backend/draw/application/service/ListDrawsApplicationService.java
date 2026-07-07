package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped draws as compact immutable query models, paginated by the repository. */
public final class ListDrawsApplicationService implements ListDrawsUseCase {

    private final DrawRepository repository;
    private final TransactionPort transaction;
    private final DrawApplicationMapper mapper;

    public ListDrawsApplicationService(
            DrawRepository repository,
            TransactionPort transaction,
            DrawApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public DrawPage<DrawSummary> execute(AggregateId tenantId, DrawPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            DrawPage<Draw> page = repository.findPage(tenantId, pageRequest);
            List<DrawSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new DrawPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
