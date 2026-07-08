package in.bachatsetu.backend.paymentgateway.application.usecase;

import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;

/** Verifies and processes one inbound gateway webhook call. */
@FunctionalInterface
public interface ProcessPaymentWebhookUseCase {

    PaymentStatusResult execute(ProcessWebhookCommand command);
}
