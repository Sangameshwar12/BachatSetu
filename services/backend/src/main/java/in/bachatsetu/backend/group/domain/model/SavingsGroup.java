package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.group.domain.event.GroupActivated;
import in.bachatsetu.backend.group.domain.event.GroupCreated;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SavingsGroup extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId organizerId;
    private final GroupCode code;
    private GroupName name;
    private final GroupType type;
    private GroupRule rule;
    private GroupStatus status;

    public SavingsGroup(
            AggregateId id,
            AggregateId tenantId,
            AggregateId organizerId,
            GroupCode code,
            GroupName name,
            GroupType type,
            GroupRule rule,
            GroupStatus status,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.organizerId = Objects.requireNonNull(organizerId, "organizerId must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.rule = Objects.requireNonNull(rule, "rule must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static SavingsGroup create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId organizerId,
            GroupCode code,
            GroupName name,
            GroupType type,
            GroupRule rule,
            Instant createdAt) {
        SavingsGroup group = new SavingsGroup(
                id,
                tenantId,
                organizerId,
                code,
                name,
                type,
                rule,
                GroupStatus.DRAFT,
                AuditInfo.createdBy(organizerId, createdAt),
                0);
        group.registerEvent(new GroupCreated(UUID.randomUUID(), id, tenantId, organizerId, createdAt));
        return group;
    }

    public void activate(int activeMemberCount, AggregateId actorId, Instant activatedAt) {
        if (status != GroupStatus.DRAFT && status != GroupStatus.PENDING_ACTIVATION) {
            throw new InvalidGroupStateException("only a draft group can be activated");
        }
        if (!rule.memberCapacity().supports(activeMemberCount)) {
            throw new InvalidGroupStateException("active member count is outside configured capacity");
        }
        status = GroupStatus.ACTIVE;
        markChanged(actorId, activatedAt);
        registerEvent(new GroupActivated(UUID.randomUUID(), id(), activeMemberCount, activatedAt));
    }

    public AggregateId tenantId() {
        return tenantId;
    }

    public AggregateId organizerId() {
        return organizerId;
    }

    public GroupCode code() {
        return code;
    }

    public GroupName name() {
        return name;
    }

    public GroupType type() {
        return type;
    }

    public GroupRule rule() {
        return rule;
    }

    public GroupStatus status() {
        return status;
    }
}
