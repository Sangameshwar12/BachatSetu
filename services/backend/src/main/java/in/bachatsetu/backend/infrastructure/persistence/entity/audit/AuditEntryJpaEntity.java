package in.bachatsetu.backend.infrastructure.persistence.entity.audit;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "audit_entries",
        schema = "audit",
        indexes = {
            @Index(name = "idx_audit_entries_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_audit_entries_actor_id", columnList = "actor_id"),
            @Index(name = "idx_audit_entries_module_name", columnList = "module_name"),
            @Index(name = "idx_audit_entries_event_type", columnList = "event_type"),
            @Index(name = "idx_audit_entries_created_at", columnList = "occurred_at")
        })
public class AuditEntryJpaEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40, updatable = false)
    private AuditEventType eventType;

    @NotBlank
    @Size(max = 60)
    @Column(name = "module_name", nullable = false, length = 60, updatable = false)
    private String moduleName;

    @Size(max = 60)
    @Column(name = "resource_type", length = 60, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "action", nullable = false, length = 120, updatable = false)
    private String action;

    @Size(max = 500)
    @Column(name = "description", length = 500, updatable = false)
    private String description;

    @Size(max = 64)
    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "user_agent", length = 255, updatable = false)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", updatable = false)
    private String metadata;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEntryJpaEntity() {
    }

    public AuditEntryJpaEntity(
            UUID id,
            UUID tenantId,
            UUID actorId,
            AuditEventType eventType,
            String moduleName,
            String resourceType,
            UUID resourceId,
            String action,
            String description,
            String ipAddress,
            String userAgent,
            String metadata,
            Instant occurredAt) {
        super(id);
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.eventType = eventType;
        this.moduleName = moduleName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getActorId() { return actorId; }
    public AuditEventType getEventType() { return eventType; }
    public String getModuleName() { return moduleName; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getMetadata() { return metadata; }
    public Instant getOccurredAt() { return occurredAt; }
}
