package in.bachatsetu.backend.storage.application.query;

import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.Objects;
import java.util.UUID;

/** Output of {@code UploadFileUseCase}: where the file ended up and under which provider. */
public record UploadFileResult(UUID fileId, StorageProvider provider, String path) {

    public UploadFileResult {
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(path, "path must not be null");
    }
}
