package in.bachatsetu.backend.receipt.application.query;

import java.util.Objects;
import java.util.UUID;

/** Where a receipt's rendered PDF ended up after being uploaded through the Storage module. */
public record ReceiptPdfStorageResult(UUID fileId, String downloadUrl) {

    public ReceiptPdfStorageResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(downloadUrl, "downloadUrl must not be null");
    }
}
