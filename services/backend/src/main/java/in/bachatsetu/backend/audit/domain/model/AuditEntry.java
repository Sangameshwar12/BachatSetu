package in.bachatsetu.backend.audit.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable record of one business action, published by any module through {@code CreateAuditEntryUseCase}
 * — never written directly by a module's own repository. Once created, an entry is never mutated; there are
 * no state-transition methods here, unlike every other aggregate in this codebase.
 *
 * <p>{@code tenantId} and {@code actorId} are both nullable: authentication events (login, OTP) happen before
 * any tenant context exists, and background/system events (a scheduled job, a startup task) have no human
 * actor. Where a real actor is unknown, {@link AuditInfo#createdBy()} still needs a non-null value for its
 * own bookkeeping, so a well-known system placeholder is used internally — it is never confused with
 * {@link #actorId()}, which stays genuinely {@code null} when no real actor exists.
 */
public final class AuditEntry extends BaseAggregateRoot {

    private static final AggregateId SYSTEM_ACTOR_PLACEHOLDER = new AggregateId(new UUID(0L, 0L));

    private final AggregateId tenantId;
    private final AggregateId actorId;
    private final AuditEventType eventType;
    private final String moduleName;
    private final String resourceType;
    private final AggregateId resourceId;
    private final String action;
    private final String description;
    private final String ipAddress;
    private final String userAgent;
    private final String metadata;
    private final Instant createdAt;

    public AuditEntry(
            AggregateId id,
            AggregateId tenantId,
            AggregateId actorId,
            AuditEventType eventType,
            String moduleName,
            String resourceType,
            AggregateId resourceId,
            String action,
            String description,
            String ipAddress,
            String userAgent,
            String metadata,
            Instant createdAt,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.moduleName = requireNonBlank(moduleName, "moduleName");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = requireNonBlank(action, "action");
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadata = metadata;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AuditEntry record(
            AggregateId id,
            AggregateId tenantId,
            AggregateId actorId,
            AuditEventType eventType,
            String moduleName,
            String resourceType,
            AggregateId resourceId,
            String action,
            String description,
            String ipAddress,
            String userAgent,
            String metadata,
            Instant createdAt) {
        AggregateId auditActor = actorId != null ? actorId : SYSTEM_ACTOR_PLACEHOLDER;
        return new AuditEntry(
                id, tenantId, actorId, eventType, moduleName, resourceType, resourceId, action, description,
                ipAddress, userAgent, metadata, createdAt, AuditInfo.createdBy(auditActor, createdAt), 0);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public AggregateId tenantId() { return tenantId; }
    public AggregateId actorId() { return actorId; }
    public AuditEventType eventType() { return eventType; }
    public String moduleName() { return moduleName; }
    public String resourceType() { return resourceType; }
    public AggregateId resourceId() { return resourceId; }
    public String action() { return action; }
    public String description() { return description; }
    public String ipAddress() { return ipAddress; }
    public String userAgent() { return userAgent; }
    public String metadata() { return metadata; }
    public Instant createdAt() { return createdAt; }
}
