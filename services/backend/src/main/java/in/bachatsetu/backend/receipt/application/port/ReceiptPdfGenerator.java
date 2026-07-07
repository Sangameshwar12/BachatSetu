package in.bachatsetu.backend.receipt.application.port;

import in.bachatsetu.backend.receipt.application.query.ReceiptResult;

/** Renders a receipt's application view as a PDF document without prescribing a rendering library. */
@FunctionalInterface
public interface ReceiptPdfGenerator {

    byte[] generate(ReceiptResult receipt);
}
