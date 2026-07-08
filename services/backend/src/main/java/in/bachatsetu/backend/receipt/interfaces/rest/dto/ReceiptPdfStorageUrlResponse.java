package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Points at a receipt PDF that has been uploaded through the Storage module. */
public record ReceiptPdfStorageUrlResponse(

        @Schema(description = "Identifier of the stored PDF file") String fileId,
        @Schema(description = "URL to download the stored PDF through the Storage module") String downloadUrl) {
}
