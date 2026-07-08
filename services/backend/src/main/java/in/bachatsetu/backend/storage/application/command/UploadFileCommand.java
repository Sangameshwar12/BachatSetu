package in.bachatsetu.backend.storage.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Input for {@code UploadFileUseCase}: a file's bytes and metadata, plus who is uploading it. */
public final class UploadFileCommand {

    private final AggregateId tenantId;
    private final String filename;
    private final String contentType;
    private final byte[] content;
    private final AggregateId actorId;

    public UploadFileCommand(
            AggregateId tenantId, String filename, String contentType, byte[] content, AggregateId actorId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.filename = requireNonBlank(filename, "filename");
        this.contentType = requireNonBlank(contentType, "contentType");
        Objects.requireNonNull(content, "content must not be null");
        this.content = content.clone();
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public AggregateId tenantId() { return tenantId; }
    public String filename() { return filename; }
    public String contentType() { return contentType; }
    public byte[] content() { return content.clone(); }
    public AggregateId actorId() { return actorId; }
}
