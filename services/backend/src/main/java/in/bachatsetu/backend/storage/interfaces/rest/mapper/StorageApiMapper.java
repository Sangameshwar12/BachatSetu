package in.bachatsetu.backend.storage.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.interfaces.rest.dto.StoredFileResponse;
import in.bachatsetu.backend.storage.interfaces.rest.dto.UploadFileResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Maps validated HTTP contracts to Storage application commands and safe responses. */
@Component
public class StorageApiMapper {

    private static final String DEFAULT_FILENAME = "upload";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public UploadFileCommand toUploadCommand(MultipartFile file, AuthenticatedUser currentUser) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        String originalFilename = file.getOriginalFilename();
        String filename = originalFilename == null || originalFilename.isBlank() ? DEFAULT_FILENAME : originalFilename;
        String rawContentType = file.getContentType();
        String contentType = rawContentType == null || rawContentType.isBlank() ? DEFAULT_CONTENT_TYPE : rawContentType;
        return new UploadFileCommand(
                currentUser.tenantId(), filename, contentType, readContent(file),
                currentUser.userId().toAggregateId());
    }

    public AggregateId toFileId(String fileId) {
        Objects.requireNonNull(fileId, "file id must not be null");
        return AggregateId.from(fileId);
    }

    public UploadFileResponse toUploadResponse(UploadFileResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new UploadFileResponse(result.fileId().toString(), result.provider().name(), result.path());
    }

    public StoredFileResponse toMetadataResponse(StoredFileResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new StoredFileResponse(
                result.fileId().toString(),
                result.tenantId().toString(),
                result.provider().name(),
                result.path(),
                result.originalFilename(),
                result.contentType(),
                result.size(),
                result.checksum(),
                result.uploadedAt());
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("unable to read uploaded file content", exception);
        }
    }
}
