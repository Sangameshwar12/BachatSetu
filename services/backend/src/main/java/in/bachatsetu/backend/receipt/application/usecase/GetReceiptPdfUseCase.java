package in.bachatsetu.backend.receipt.application.usecase;

import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Renders one tenant-scoped receipt as a downloadable PDF document. */
@FunctionalInterface
public interface GetReceiptPdfUseCase {

    ReceiptPdfResult execute(AggregateId tenantId, AggregateId receiptId);
}
