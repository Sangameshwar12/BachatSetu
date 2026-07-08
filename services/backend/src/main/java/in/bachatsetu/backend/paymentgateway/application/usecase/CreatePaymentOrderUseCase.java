package in.bachatsetu.backend.paymentgateway.application.usecase;

import in.bachatsetu.backend.paymentgateway.application.command.CreatePaymentOrderCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;

/** Creates a provider order for an existing payment. */
@FunctionalInterface
public interface CreatePaymentOrderUseCase {

    PaymentOrderResult execute(CreatePaymentOrderCommand command);
}
