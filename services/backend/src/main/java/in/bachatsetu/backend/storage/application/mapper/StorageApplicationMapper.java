package in.bachatsetu.backend.storage.application.mapper;

import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import java.util.Objects;

/** Converts the {@link StoredFile} aggregate to application-layer read models. */
public final class StorageApplicationMapper {

    public UploadFileResult toUploadResult(StoredFile file) {
        Objects.requireNonNull(file, "file must not be null");
        return new UploadFileResult(file.id().value(), file.provider(), file.path());
    }

    public StoredFileResult toMetadataResult(StoredFile file) {
        Objects.requireNonNull(file, "file must not be null");
        return new StoredFileResult(
                file.id().value(),
                file.tenantId().value(),
                file.provider(),
                file.path(),
                file.originalFilename(),
                file.contentType(),
                file.size(),
                file.checksum(),
                file.uploadedAt());
    }
}
