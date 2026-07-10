package in.bachatsetu.backend.infrastructure.persistence.entity.config;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feature_flags", schema = "config")
public class FeatureFlagJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_key", nullable = false, updatable = false, length = 50)
    private FeatureKey featureKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected FeatureFlagJpaEntity() {
    }

    public FeatureFlagJpaEntity(FeatureKey featureKey, boolean enabled, long version, Instant updatedAt, UUID updatedBy) {
        this.featureKey = featureKey;
        this.enabled = enabled;
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public void update(boolean enabled, long version, Instant updatedAt, UUID updatedBy) {
        this.enabled = enabled;
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public FeatureKey getFeatureKey() {
        return featureKey;
    }

    public boolean isEnabled() {
        return enabled;
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
