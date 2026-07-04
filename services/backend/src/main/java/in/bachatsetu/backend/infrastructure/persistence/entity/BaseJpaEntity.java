package in.bachatsetu.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    protected BaseJpaEntity() {
    }

    protected BaseJpaEntity(UUID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    protected final void markDeleted(UUID actorId, Instant deletedAt) {
        this.deletedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
        this.deleted = true;
    }

    protected final void restore() {
        deleted = false;
        deletedAt = null;
        deletedBy = null;
    }

    public final void copyPersistenceStateFrom(BaseJpaEntity source) {
        Objects.requireNonNull(source, "source must not be null");
        if (!Objects.equals(id, source.id)) {
            throw new IllegalArgumentException("persistence identity must match");
        }
        createdAt = source.createdAt;
        createdBy = source.createdBy;
        updatedAt = source.updatedAt;
        updatedBy = source.updatedBy;
        version = source.version;
        deleted = source.deleted;
        deletedAt = source.deletedAt;
        deletedBy = source.deletedBy;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        BaseJpaEntity that = (BaseJpaEntity) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
