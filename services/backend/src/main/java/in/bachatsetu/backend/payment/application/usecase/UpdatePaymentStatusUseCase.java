package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;

/** Transitions an existing payment to a new lifecycle status. */
@FunctionalInterface
public interface UpdatePaymentStatusUseCase {

    PaymentResult execute(UpdatePaymentStatusCommand command);
}
