package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;

/** Initiates a payment and returns its current state. */
@FunctionalInterface
public interface CreatePaymentUseCase {

    PaymentResult execute(CreatePaymentCommand command);
}
