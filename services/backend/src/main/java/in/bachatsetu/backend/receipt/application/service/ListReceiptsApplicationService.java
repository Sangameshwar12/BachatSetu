package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped receipts as compact immutable query models, paginated by the repository. */
public final class ListReceiptsApplicationService implements ListReceiptsUseCase {

    private final ReceiptRepository repository;
    private final TransactionPort transaction;
    private final ReceiptApplicationMapper mapper;

    public ListReceiptsApplicationService(
            ReceiptRepository repository,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public ReceiptPage<ReceiptSummary> execute(AggregateId tenantId, ReceiptPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            ReceiptPage<Receipt> page = repository.findPage(tenantId, pageRequest);
            List<ReceiptSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new ReceiptPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
