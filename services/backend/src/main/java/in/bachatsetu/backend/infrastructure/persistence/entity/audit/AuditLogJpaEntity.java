package in.bachatsetu.backend.infrastructure.persistence.entity.audit;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        schema = "audit",
        indexes = {
            @Index(name = "idx_audit_logs_tenant_occurred", columnList = "tenant_id,occurred_at"),
            @Index(name = "idx_audit_logs_resource", columnList = "resource_type,resource_id"),
            @Index(name = "idx_audit_logs_action", columnList = "action_code")
        })
public class AuditLogJpaEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", updatable = false)
    private UserJpaEntity actor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, updatable = false, length = 20)
    private AuditActorType actorType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "action_code", nullable = false, updatable = false, length = 100)
    private String actionCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "resource_type", nullable = false, updatable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Size(max = 100)
    @Column(name = "reason_code", updatable = false, length = 100)
    private String reasonCode;

    @NotNull
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Size(max = 128)
    @Column(name = "ip_hash", updatable = false, length = 128)
    private String ipHash;

    @Size(max = 128)
    @Column(name = "user_agent_hash", updatable = false, length = 128)
    private String userAgentHash;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditLogJpaEntity() {
    }

    public AuditLogJpaEntity(
            UUID id, UUID tenantId, UserJpaEntity actor, AuditActorType actorType,
            String actionCode, String resourceType, UUID resourceId, String reasonCode,
            UUID requestId, String ipHash, String userAgentHash, Instant occurredAt) {
        super(id);
        this.tenantId = tenantId;
        this.actor = actor;
        this.actorType = actorType;
        this.actionCode = actionCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.reasonCode = reasonCode;
        this.requestId = requestId;
        this.ipHash = ipHash;
        this.userAgentHash = userAgentHash;
        this.occurredAt = occurredAt;
    }

    public UUID getTenantId() { return tenantId; }
    public UserJpaEntity getActor() { return actor; }
    public AuditActorType getActorType() { return actorType; }
    public String getActionCode() { return actionCode; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getReasonCode() { return reasonCode; }
    public UUID getRequestId() { return requestId; }
    public String getIpHash() { return ipHash; }
    public String getUserAgentHash() { return userAgentHash; }
    public Instant getOccurredAt() { return occurredAt; }
}
