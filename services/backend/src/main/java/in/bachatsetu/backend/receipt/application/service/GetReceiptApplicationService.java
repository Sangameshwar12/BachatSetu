package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.exception.ReceiptNotFoundException;
import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Receipt aggregate. */
public final class GetReceiptApplicationService implements GetReceiptUseCase {

    private final ReceiptRepository repository;
    private final TransactionPort transaction;
    private final ReceiptApplicationMapper mapper;

    public GetReceiptApplicationService(
            ReceiptRepository repository,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public ReceiptResult execute(AggregateId tenantId, AggregateId receiptId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        return transaction.execute(() -> {
            Receipt receipt = repository.findById(tenantId, receiptId)
                    .orElseThrow(() -> new ReceiptNotFoundException("receipt does not exist"));
            return mapper.toResult(receipt);
        });
    }
}
