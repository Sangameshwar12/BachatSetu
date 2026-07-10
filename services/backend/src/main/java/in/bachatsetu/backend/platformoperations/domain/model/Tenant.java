package in.bachatsetu.backend.platformoperations.domain.model;

import in.bachatsetu.backend.platformoperations.domain.event.TenantActivated;
import in.bachatsetu.backend.platformoperations.domain.event.TenantArchived;
import in.bachatsetu.backend.platformoperations.domain.event.TenantSuspended;
import in.bachatsetu.backend.platformoperations.domain.exception.PlatformOperationsDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Platform-wide lifecycle record for a tenant. This codebase has no other Tenant aggregate — tenants are
 * otherwise only inferred from the distinct {@code tenant_id} values recorded on other modules' tables (see
 * {@code admin.domain.model.PlatformTenantSummary}) — so a {@link Tenant} row is created lazily, on first
 * platform-operations action against a tenant, defaulting to {@link TenantStatus#ACTIVE}.
 */
public final class Tenant extends BaseAggregateRoot {

    private TenantStatus status;
    private String suspensionReason;

    private Tenant(AggregateId id, TenantStatus status, String suspensionReason, AuditInfo auditInfo, long version) {
        super(id, auditInfo, version);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.suspensionReason = suspensionReason;
    }

    public static Tenant createActive(AggregateId id, AggregateId actorId, Instant createdAt) {
        return new Tenant(id, TenantStatus.ACTIVE, null, AuditInfo.createdBy(actorId, createdAt), 0);
    }

    public static Tenant rehydrate(
            AggregateId id, TenantStatus status, String suspensionReason, AuditInfo auditInfo, long version) {
        return new Tenant(id, status, suspensionReason, auditInfo, version);
    }

    public void suspend(String reason, AggregateId actorId, Instant suspendedAt) {
        if (status != TenantStatus.ACTIVE) {
            throw new PlatformOperationsDomainException("only an active tenant can be suspended");
        }
        this.status = TenantStatus.SUSPENDED;
        this.suspensionReason = reason;
        markChanged(actorId, suspendedAt);
        registerEvent(new TenantSuspended(UUID.randomUUID(), id(), reason, suspendedAt));
    }

    public void activate(AggregateId actorId, Instant activatedAt) {
        if (status != TenantStatus.SUSPENDED) {
            throw new PlatformOperationsDomainException("only a suspended tenant can be activated");
        }
        this.status = TenantStatus.ACTIVE;
        this.suspensionReason = null;
        markChanged(actorId, activatedAt);
        registerEvent(new TenantActivated(UUID.randomUUID(), id(), activatedAt));
    }

    public void archive(AggregateId actorId, Instant archivedAt) {
        if (status == TenantStatus.ARCHIVED) {
            throw new PlatformOperationsDomainException("tenant is already archived");
        }
        this.status = TenantStatus.ARCHIVED;
        markChanged(actorId, archivedAt);
        registerEvent(new TenantArchived(UUID.randomUUID(), id(), archivedAt));
    }

    public TenantStatus status() {
        return status;
    }

    public String suspensionReason() {
        return suspensionReason;
    }
}
