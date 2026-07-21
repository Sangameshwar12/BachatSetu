package in.bachatsetu.backend.payment.application.usecase;

import in.bachatsetu.backend.payment.application.command.RecordManualPaymentCommand;
import in.bachatsetu.backend.payment.application.query.PaymentResult;

/** Organizer-only: records a member's contribution for the group's current cycle as collected in cash. */
@FunctionalInterface
public interface RecordManualPaymentUseCase {

    PaymentResult execute(RecordManualPaymentCommand command);
}
