package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped payment. */
@FunctionalInterface
public interface GetPaymentUseCase {

    PaymentResult execute(AggregateId tenantId, AggregateId paymentId);
}
