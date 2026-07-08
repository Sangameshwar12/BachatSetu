package in.bachatsetu.backend.paymentgateway.application.usecase;

import in.bachatsetu.backend.paymentgateway.application.command.InitiateRefundCommand;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;

/** Initiates a full refund of a {@code VERIFIED} payment. */
@FunctionalInterface
public interface InitiateRefundUseCase {

    RefundResult execute(InitiateRefundCommand command);
}
