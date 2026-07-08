package in.bachatsetu.backend.storage.application.query;

import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Full metadata for one stored file, as returned by {@code GetFileMetadataUseCase}. */
public record StoredFileResult(
        UUID fileId,
        UUID tenantId,
        StorageProvider provider,
        String path,
        String originalFilename,
        String contentType,
        long size,
        String checksum,
        Instant uploadedAt) {

    public StoredFileResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");
        Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
    }
}
