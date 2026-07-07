package in.bachatsetu.backend.receipt.application.usecase;

import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact receipt views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListReceiptsUseCase {

    ReceiptPage<ReceiptSummary> execute(AggregateId tenantId, ReceiptPageRequest pageRequest);
}
