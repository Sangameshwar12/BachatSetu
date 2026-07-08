package in.bachatsetu.backend.storage.application.query;

import java.util.Objects;
import java.util.UUID;

/** Output of {@code DownloadFileUseCase}: a file's bytes paired with the metadata needed to serve them. */
public final class FileDownloadResult {

    private final UUID fileId;
    private final String filename;
    private final String contentType;
    private final byte[] content;

    public FileDownloadResult(UUID fileId, String filename, String contentType, byte[] content) {
        this.fileId = Objects.requireNonNull(fileId, "fileId must not be null");
        this.filename = Objects.requireNonNull(filename, "filename must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(content, "content must not be null");
        this.content = content.clone();
    }

    public UUID fileId() { return fileId; }
    public String filename() { return filename; }
    public String contentType() { return contentType; }
    public byte[] content() { return content.clone(); }
}
