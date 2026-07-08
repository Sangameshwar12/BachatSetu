package in.bachatsetu.backend.storage.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Minimal confirmation returned right after a successful upload. */
public record UploadFileResponse(

        @Schema(description = "File identifier") String fileId,
        @Schema(description = "Storage provider the file was uploaded to", example = "LOCAL") String provider,
        @Schema(description = "Provider-specific path or key the file is stored under") String path) {
}
