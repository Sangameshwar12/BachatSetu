package in.bachatsetu.backend.infrastructure.persistence.entity.storage;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stored_files",
        schema = "storage",
        indexes = @Index(name = "idx_stored_files_tenant", columnList = "tenant_id"))
public class StoredFileJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30, updatable = false)
    private StorageProvider provider;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "path", nullable = false, length = 1000, updatable = false)
    private String path;

    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false, length = 255, updatable = false)
    private String filename;

    @NotBlank
    @Size(max = 255)
    @Column(name = "content_type", nullable = false, length = 255, updatable = false)
    private String contentType;

    @NotBlank
    @Size(max = 128)
    @Column(name = "checksum", nullable = false, length = 128, updatable = false)
    private String checksum;

    @PositiveOrZero
    @Column(name = "size", nullable = false, updatable = false)
    private long size;

    @NotNull
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected StoredFileJpaEntity() {
    }

    public StoredFileJpaEntity(
            UUID id,
            UUID tenantId,
            StorageProvider provider,
            String path,
            String filename,
            String contentType,
            String checksum,
            long size,
            Instant uploadedAt) {
        super(id);
        this.tenantId = tenantId;
        this.provider = provider;
        this.path = path;
        this.filename = filename;
        this.contentType = contentType;
        this.checksum = checksum;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    public UUID getTenantId() { return tenantId; }
    public StorageProvider getProvider() { return provider; }
    public String getPath() { return path; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public String getChecksum() { return checksum; }
    public long getSize() { return size; }
    public Instant getUploadedAt() { return uploadedAt; }
}
