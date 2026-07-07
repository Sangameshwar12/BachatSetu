package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact payment views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListPaymentsUseCase {

    PaymentPage<PaymentSummary> execute(AggregateId tenantId, PaymentPageRequest pageRequest);
}
