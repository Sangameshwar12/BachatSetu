package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Loads a tenant-scoped receipt through the existing read use case and renders it as a PDF. */
public final class GetReceiptPdfApplicationService implements GetReceiptPdfUseCase {

    private final GetReceiptUseCase getReceipt;
    private final ReceiptPdfGenerator pdfGenerator;

    public GetReceiptPdfApplicationService(GetReceiptUseCase getReceipt, ReceiptPdfGenerator pdfGenerator) {
        this.getReceipt = Objects.requireNonNull(getReceipt, "get receipt use case must not be null");
        this.pdfGenerator = Objects.requireNonNull(pdfGenerator, "pdf generator must not be null");
    }

    @Override
    public ReceiptPdfResult execute(AggregateId tenantId, AggregateId receiptId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        ReceiptResult receipt = getReceipt.execute(tenantId, receiptId);
        byte[] content = pdfGenerator.generate(receipt);
        String fileName = receipt.number().replace("/", "-") + ".pdf";
        return new ReceiptPdfResult(content, fileName);
    }
}
