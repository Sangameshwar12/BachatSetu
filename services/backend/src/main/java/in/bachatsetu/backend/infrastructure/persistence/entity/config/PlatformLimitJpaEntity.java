package in.bachatsetu.backend.infrastructure.persistence.entity.config;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_limits", schema = "config")
public class PlatformLimitJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "limit_key", nullable = false, updatable = false, length = 50)
    private LimitKey limitKey;

    @Column(name = "limit_value", nullable = false)
    private long limitValue;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected PlatformLimitJpaEntity() {
    }

    public PlatformLimitJpaEntity(LimitKey limitKey, long limitValue, long version, Instant updatedAt, UUID updatedBy) {
        this.limitKey = limitKey;
        this.limitValue = limitValue;
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public void update(long limitValue, long version, Instant updatedAt, UUID updatedBy) {
        this.limitValue = limitValue;
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public LimitKey getLimitKey() {
        return limitKey;
    }

    public long getLimitValue() {
        return limitValue;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
