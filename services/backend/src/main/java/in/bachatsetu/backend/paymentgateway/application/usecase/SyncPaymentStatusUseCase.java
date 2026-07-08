package in.bachatsetu.backend.paymentgateway.application.usecase;

import in.bachatsetu.backend.paymentgateway.application.command.SyncPaymentStatusCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;

/** Pulls a payment's current status from its gateway, for when a webhook may have been missed. */
@FunctionalInterface
public interface SyncPaymentStatusUseCase {

    PaymentStatusResult execute(SyncPaymentStatusCommand command);
}
