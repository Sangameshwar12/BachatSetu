package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.group.domain.event.GroupActivated;
import in.bachatsetu.backend.group.domain.event.GroupClosed;
import in.bachatsetu.backend.group.domain.event.GroupSuspended;
import in.bachatsetu.backend.group.domain.event.MemberJoined;
import in.bachatsetu.backend.group.domain.event.MemberRemoved;
import in.bachatsetu.backend.group.domain.event.SavingsGroupCreated;
import in.bachatsetu.backend.group.domain.exception.DuplicateMemberException;
import in.bachatsetu.backend.group.domain.exception.GroupCapacityExceededException;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.group.domain.exception.OwnerRemovalNotAllowedException;
import in.bachatsetu.backend.group.domain.service.GroupValidationService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root governing a Bhishi savings group, its lifecycle, and membership invariants.
 */
public final class SavingsGroup extends BaseAggregateRoot {

    private static final GroupValidationService VALIDATION_SERVICE = new GroupValidationService();

    private final AggregateId tenantId;
    private final OwnerId ownerId;
    private final GroupCode code;
    private final GroupName name;
    private final GroupDescription description;
    private final GroupType type;
    private final GroupRule rule;
    private final ContributionAmount contributionAmount;
    private final MaximumMembers maximumMembers;
    private final List<GroupMember> members;
    private GroupStatus status;

    /**
     * Reconstructs the legacy persistence projection without emitting domain events.
     * New domain behavior should use {@link #create} or {@link #rehydrate}.
     */
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
        this(
                new GroupId(id),
                tenantId,
                new OwnerId(organizerId),
                code,
                name,
                GroupDescription.empty(),
                type,
                rule,
                status,
                List.of(GroupMember.join(organizerId, new CreatedAt(auditInfo.createdAt()))),
                auditInfo,
                version);
    }

    private SavingsGroup(
            GroupId groupId,
            AggregateId tenantId,
            OwnerId ownerId,
            GroupCode code,
            GroupName name,
            GroupDescription description,
            GroupType type,
            GroupRule rule,
            GroupStatus status,
            List<GroupMember> members,
            AuditInfo auditInfo,
            long version) {
        super(Objects.requireNonNull(groupId, "group id must not be null").value(), auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenant id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "owner id must not be null");
        this.code = Objects.requireNonNull(code, "group code must not be null");
        this.name = Objects.requireNonNull(name, "group name must not be null");
        this.description = Objects.requireNonNull(description, "group description must not be null");
        this.type = Objects.requireNonNull(type, "group type must not be null");
        this.rule = Objects.requireNonNull(rule, "group rule must not be null");
        this.status = Objects.requireNonNull(status, "group status must not be null");
        this.contributionAmount = new ContributionAmount(rule.contributionSchedule().contributionAmount());
        this.maximumMembers = new MaximumMembers(rule.memberCapacity().maximum());
        this.members = new ArrayList<>(Objects.requireNonNull(members, "members must not be null"));
        VALIDATION_SERVICE.validateCreation(ownerId, contributionAmount, maximumMembers, rule);
        VALIDATION_SERVICE.validateMemberships(ownerId, maximumMembers, this.members);
    }

    /** Creates a new inactive savings group and enrolls its owner as the first member. */
    public static SavingsGroup create(
            GroupId groupId,
            AggregateId tenantId,
            OwnerId ownerId,
            GroupCode code,
            GroupName name,
            GroupDescription description,
            GroupType type,
            GroupRule rule,
            CreatedAt createdAt) {
        Objects.requireNonNull(createdAt, "created at must not be null");
        GroupMember owner = GroupMember.join(ownerId.value(), createdAt);
        SavingsGroup group = new SavingsGroup(
                groupId,
                tenantId,
                ownerId,
                code,
                name,
                description,
                type,
                rule,
                GroupStatus.INACTIVE,
                List.of(owner),
                AuditInfo.createdBy(ownerId.value(), createdAt.value()),
                0);
        group.registerEvent(new SavingsGroupCreated(
                UUID.randomUUID(), groupId.value(), tenantId, ownerId, code, createdAt.value()));
        return group;
    }

    /** Compatibility factory for callers that use shared identifiers directly. */
    public static SavingsGroup create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId organizerId,
            GroupCode code,
            GroupName name,
            GroupType type,
            GroupRule rule,
            Instant createdAt) {
        return create(
                new GroupId(id),
                tenantId,
                new OwnerId(organizerId),
                code,
                name,
                GroupDescription.empty(),
                type,
                rule,
                new CreatedAt(createdAt));
    }

    /** Reconstructs a complete aggregate without producing domain events. */
    public static SavingsGroup rehydrate(
            GroupId groupId,
            AggregateId tenantId,
            OwnerId ownerId,
            GroupCode code,
            GroupName name,
            GroupDescription description,
            GroupType type,
            GroupRule rule,
            GroupStatus status,
            List<GroupMember> members,
            AuditInfo auditInfo,
            long version) {
        return new SavingsGroup(
                groupId,
                tenantId,
                ownerId,
                code,
                name,
                description,
                type,
                rule,
                status,
                members,
                auditInfo,
                version);
    }

    /** Adds a member when the group is active and capacity permits. */
    public GroupMember joinMember(AggregateId memberId, AggregateId actorId, Instant joinedAt) {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(joinedAt, "joined at must not be null");
        requireStatus(GroupStatus.ACTIVE, "only an active group can accept members");
        if (members.stream().anyMatch(member -> member.memberId().equals(memberId))) {
            throw new DuplicateMemberException("member has already joined the group");
        }
        if (!maximumMembers.accommodates(memberCount().increment())) {
            throw new GroupCapacityExceededException("group has reached its maximum member capacity");
        }
        GroupMember member = GroupMember.join(memberId, new CreatedAt(joinedAt));
        members.add(member);
        markChanged(actorId, joinedAt);
        registerEvent(new MemberJoined(UUID.randomUUID(), id(), memberId, joinedAt));
        return member;
    }

    /** Removes an active non-owner member while retaining membership history. */
    public void removeMember(AggregateId memberId, AggregateId actorId, Instant removedAt) {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(removedAt, "removed at must not be null");
        if (ownerId.value().equals(memberId)) {
            throw new OwnerRemovalNotAllowedException("the group owner cannot be removed");
        }
        if (status == GroupStatus.CLOSED) {
            throw new InvalidGroupStateException("a closed group cannot remove members");
        }
        int memberIndex = activeMemberIndex(memberId);
        if (memberIndex < 0) {
            throw new InvalidGroupStateException("member is not active in the group");
        }
        members.set(memberIndex, members.get(memberIndex).remove(new UpdatedAt(removedAt)));
        markChanged(actorId, removedAt);
        registerEvent(new MemberRemoved(UUID.randomUUID(), id(), memberId, removedAt));
    }

    /** Activates an inactive or suspended group. */
    public void activate(AggregateId actorId, Instant activatedAt) {
        transitionTo(GroupStatus.ACTIVE, actorId, activatedAt);
        registerEvent(new GroupActivated(UUID.randomUUID(), id(), memberCount().value(), activatedAt));
    }

    /** Suspends an active group. */
    public void suspend(AggregateId actorId, Instant suspendedAt) {
        transitionTo(GroupStatus.SUSPENDED, actorId, suspendedAt);
        registerEvent(new GroupSuspended(UUID.randomUUID(), id(), suspendedAt));
    }

    /** Permanently closes an active or inactive group. */
    public void close(AggregateId actorId, Instant closedAt) {
        transitionTo(GroupStatus.CLOSED, actorId, closedAt);
        registerEvent(new GroupClosed(UUID.randomUUID(), id(), closedAt));
    }

    public GroupId groupId() {
        return new GroupId(id());
    }

    public AggregateId tenantId() {
        return tenantId;
    }

    public OwnerId ownerId() {
        return ownerId;
    }

    public AggregateId organizerId() {
        return ownerId.value();
    }

    public GroupCode code() {
        return code;
    }

    public GroupName name() {
        return name;
    }

    public GroupDescription description() {
        return description;
    }

    public ContributionAmount contributionAmount() {
        return contributionAmount;
    }

    public MaximumMembers maximumMembers() {
        return maximumMembers;
    }

    public MemberCount memberCount() {
        return new MemberCount((int) members.stream().filter(GroupMember::isActive).count());
    }

    public List<GroupMember> members() {
        return List.copyOf(members);
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

    public CreatedAt createdAt() {
        return new CreatedAt(auditInfo().createdAt());
    }

    public UpdatedAt updatedAt() {
        return new UpdatedAt(auditInfo().updatedAt());
    }

    private int activeMemberIndex(AggregateId memberId) {
        for (int index = 0; index < members.size(); index++) {
            GroupMember member = members.get(index);
            if (member.memberId().equals(memberId) && member.isActive()) {
                return index;
            }
        }
        return -1;
    }

    private void transitionTo(GroupStatus target, AggregateId actorId, Instant changedAt) {
        Objects.requireNonNull(target, "target status must not be null");
        Objects.requireNonNull(changedAt, "changed at must not be null");
        if (!isAllowedTransition(status, target)) {
            throw new InvalidGroupStateException("group cannot transition from " + status + " to " + target);
        }
        markChanged(actorId, changedAt);
        status = target;
    }

    private void requireStatus(GroupStatus requiredStatus, String message) {
        if (status != requiredStatus) {
            throw new InvalidGroupStateException(message);
        }
    }

    private static boolean isAllowedTransition(GroupStatus current, GroupStatus target) {
        return current == GroupStatus.ACTIVE
                        && (target == GroupStatus.CLOSED || target == GroupStatus.SUSPENDED)
                || current == GroupStatus.SUSPENDED && target == GroupStatus.ACTIVE
                || current == GroupStatus.INACTIVE
                        && (target == GroupStatus.ACTIVE || target == GroupStatus.CLOSED);
    }
}
