package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfStorageResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfStorageUrlUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import java.util.Objects;

/**
 * Chains the pre-existing {@link GetReceiptPdfUseCase} (unchanged, still callable and returning the same
 * bytes on its own) into the Storage module's {@link UploadFileUseCase}, then hands back a URL pointing at
 * the Storage module's own download endpoint rather than any provider-internal path — callers never need to
 * know which storage provider is configured.
 */
public final class GetReceiptPdfStorageUrlApplicationService implements GetReceiptPdfStorageUrlUseCase {

    private static final String CONTENT_TYPE = "application/pdf";
    private static final String DOWNLOAD_URL_PREFIX = "/api/v1/storage/files/";
    private static final String DOWNLOAD_URL_SUFFIX = "/download";

    private final GetReceiptPdfUseCase getReceiptPdf;
    private final UploadFileUseCase uploadFile;

    public GetReceiptPdfStorageUrlApplicationService(GetReceiptPdfUseCase getReceiptPdf, UploadFileUseCase uploadFile) {
        this.getReceiptPdf = Objects.requireNonNull(getReceiptPdf, "get receipt pdf use case must not be null");
        this.uploadFile = Objects.requireNonNull(uploadFile, "upload file use case must not be null");
    }

    @Override
    public ReceiptPdfStorageResult execute(AggregateId tenantId, AggregateId receiptId, AggregateId actorId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        ReceiptPdfResult pdf = getReceiptPdf.execute(tenantId, receiptId);
        UploadFileCommand command = new UploadFileCommand(tenantId, pdf.fileName(), CONTENT_TYPE, pdf.content(), actorId);
        UploadFileResult uploaded = uploadFile.execute(command);
        String downloadUrl = DOWNLOAD_URL_PREFIX + uploaded.fileId() + DOWNLOAD_URL_SUFFIX;
        return new ReceiptPdfStorageResult(uploaded.fileId(), downloadUrl);
    }
}
