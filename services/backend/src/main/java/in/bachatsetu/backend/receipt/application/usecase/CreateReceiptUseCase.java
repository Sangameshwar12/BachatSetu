package in.bachatsetu.backend.receipt.application.usecase;

import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;

/** Generates a receipt and returns its current state. */
@FunctionalInterface
public interface CreateReceiptUseCase {

    ReceiptResult execute(CreateReceiptCommand command);
}
