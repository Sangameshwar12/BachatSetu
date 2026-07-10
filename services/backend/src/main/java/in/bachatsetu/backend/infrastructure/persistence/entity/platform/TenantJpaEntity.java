package in.bachatsetu.backend.infrastructure.persistence.entity.platform;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Canonical persistence representation of the Tenant lifecycle aggregate. The row's {@code id} is the same
 * tenant identifier already recorded as {@code tenant_id} on every other tenant-scoped table — there is no
 * foreign key, since no single canonical tenant table existed before this module.
 */
@Entity
@Table(name = "tenants", schema = "platform")
public class TenantJpaEntity extends BaseJpaEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    protected TenantJpaEntity() {
    }

    public TenantJpaEntity(UUID id, TenantStatus status, String suspensionReason) {
        super(id);
        this.status = status;
        this.suspensionReason = suspensionReason;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void update(TenantStatus newStatus, String newSuspensionReason) {
        this.status = newStatus;
        this.suspensionReason = newSuspensionReason;
    }
}
