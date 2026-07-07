package in.bachatsetu.backend.receipt.application.usecase;

import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped receipt. */
@FunctionalInterface
public interface GetReceiptUseCase {

    ReceiptResult execute(AggregateId tenantId, AggregateId receiptId);
}
