package in.bachatsetu.backend.storage.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * Metadata for one binary file stored through the storage module, independent of which provider actually
 * holds the bytes. {@code path} is the provider-specific key/location returned by the {@code StoragePort}
 * that performed the upload — this aggregate never touches the bytes themselves, only remembers where they
 * are and what they were.
 *
 * <p>Deliberately holds no reference to any business module (Payment, Receipt, ...): callers identify their
 * own files by {@link #id()} once uploaded, following this codebase's rule that cross-module relationships
 * are identifiers, not object references.
 */
public final class StoredFile extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final StorageProvider provider;
    private final String path;
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final String checksum;
    private final Instant uploadedAt;

    public StoredFile(
            AggregateId id,
            AggregateId tenantId,
            StorageProvider provider,
            String path,
            String originalFilename,
            String contentType,
            long size,
            String checksum,
            Instant uploadedAt,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.path = requireNonBlank(path, "path");
        this.originalFilename = requireNonBlank(originalFilename, "originalFilename");
        this.contentType = requireNonBlank(contentType, "contentType");
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        this.size = size;
        this.checksum = requireNonBlank(checksum, "checksum");
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
    }

    public static StoredFile upload(
            AggregateId id,
            AggregateId tenantId,
            StorageProvider provider,
            String path,
            String originalFilename,
            String contentType,
            long size,
            String checksum,
            AggregateId actorId,
            Instant uploadedAt) {
        return new StoredFile(
                id, tenantId, provider, path, originalFilename, contentType, size, checksum, uploadedAt,
                AuditInfo.createdBy(actorId, uploadedAt), 0);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public AggregateId tenantId() { return tenantId; }
    public StorageProvider provider() { return provider; }
    public String path() { return path; }
    public String originalFilename() { return originalFilename; }
    public String contentType() { return contentType; }
    public long size() { return size; }
    public String checksum() { return checksum; }
    public Instant uploadedAt() { return uploadedAt; }
}
