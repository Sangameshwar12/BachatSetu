package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
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
    private final CreateAuditEntryUseCase createAuditEntry;

    public GetReceiptPdfApplicationService(
            GetReceiptUseCase getReceipt, ReceiptPdfGenerator pdfGenerator, CreateAuditEntryUseCase createAuditEntry) {
        this.getReceipt = Objects.requireNonNull(getReceipt, "get receipt use case must not be null");
        this.pdfGenerator = Objects.requireNonNull(pdfGenerator, "pdf generator must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public ReceiptPdfResult execute(AggregateId tenantId, AggregateId receiptId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        ReceiptResult receipt = getReceipt.execute(tenantId, receiptId);
        byte[] content = pdfGenerator.generate(receipt);
        String fileName = receipt.number().replace("/", "-") + ".pdf";
        auditPdfDownloaded(receipt);
        return new ReceiptPdfResult(content, fileName);
    }

    /**
     * Best-effort: an audit failure must never fail a PDF that has already been rendered, so any exception is
     * caught and discarded here rather than propagated.
     */
    private void auditPdfDownloaded(ReceiptResult receipt) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    new AggregateId(receipt.tenantId()), new AggregateId(receipt.memberId()),
                    AuditEventType.PDF_DOWNLOADED, "receipt", "Receipt", new AggregateId(receipt.receiptId()),
                    "PDF_DOWNLOADED", "receipt pdf downloaded", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-rendered PDF.
        }
    }
}
