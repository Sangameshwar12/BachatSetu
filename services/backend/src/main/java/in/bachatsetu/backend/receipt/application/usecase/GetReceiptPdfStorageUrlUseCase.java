package in.bachatsetu.backend.receipt.application.usecase;

import in.bachatsetu.backend.receipt.application.query.ReceiptPdfStorageResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/**
 * Renders one tenant-scoped receipt as a PDF, uploads it through the Storage module, and returns a download
 * URL. Optional integration on top of {@link GetReceiptPdfUseCase}, which this use case calls unchanged and
 * never replaces — disabled by default via {@code bachatsetu.receipt.storage-upload.enabled}.
 */
@FunctionalInterface
public interface GetReceiptPdfStorageUrlUseCase {

    ReceiptPdfStorageResult execute(AggregateId tenantId, AggregateId receiptId, AggregateId actorId);
}
