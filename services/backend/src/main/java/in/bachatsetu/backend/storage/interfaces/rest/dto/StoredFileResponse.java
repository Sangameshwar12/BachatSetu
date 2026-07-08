package in.bachatsetu.backend.storage.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of a stored file's metadata returned by the REST API. */
public record StoredFileResponse(

        @Schema(description = "File identifier") String fileId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Storage provider the file was uploaded to", example = "LOCAL") String provider,
        @Schema(description = "Provider-specific path or key the file is stored under") String path,
        @Schema(description = "Original uploaded file name") String filename,
        @Schema(description = "MIME content type") String contentType,
        @Schema(description = "File size in bytes") long size,
        @Schema(description = "SHA-256 hex checksum of the file's bytes") String checksum,
        @Schema(description = "Timestamp the file was uploaded") Instant uploadedAt) {
}
