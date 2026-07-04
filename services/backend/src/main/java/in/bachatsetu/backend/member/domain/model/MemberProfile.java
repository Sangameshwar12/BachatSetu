package in.bachatsetu.backend.member.domain.model;

import in.bachatsetu.backend.member.domain.event.MemberCreated;
import in.bachatsetu.backend.member.domain.event.MemberJoinedGroup;
import in.bachatsetu.backend.member.domain.exception.DuplicateGroupParticipationException;
import in.bachatsetu.backend.member.domain.exception.InvalidMembershipStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MemberProfile extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId userId;
    private final MemberNumber memberNumber;
    private MemberStatus status;
    private final List<GroupParticipation> participations;
    private final List<MemberConsent> consents;

    public MemberProfile(
            AggregateId id,
            AggregateId tenantId,
            AggregateId userId,
            MemberNumber memberNumber,
            MemberStatus status,
            List<GroupParticipation> participations,
            List<MemberConsent> consents,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.memberNumber = Objects.requireNonNull(memberNumber, "memberNumber must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.participations = new ArrayList<>(Objects.requireNonNull(participations, "participations must not be null"));
        this.consents = new ArrayList<>(Objects.requireNonNull(consents, "consents must not be null"));
    }

    public static MemberProfile create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId userId,
            MemberNumber memberNumber,
            AggregateId actorId,
            Instant createdAt) {
        MemberProfile member = new MemberProfile(
                id,
                tenantId,
                userId,
                memberNumber,
                MemberStatus.INVITED,
                List.of(),
                List.of(),
                AuditInfo.createdBy(actorId, createdAt),
                0);
        member.registerEvent(new MemberCreated(UUID.randomUUID(), id, tenantId, userId, createdAt));
        return member;
    }

    public GroupParticipation joinGroup(
            AggregateId groupId,
            GroupRole role,
            AggregateId actorId,
            Instant joinedAt) {
        if (status == MemberStatus.REMOVED || status == MemberStatus.EXITED) {
            throw new InvalidMembershipStateException("inactive member cannot join a group");
        }
        boolean alreadyParticipating = participations.stream().anyMatch(participation ->
                participation.groupId().equals(groupId)
                        && participation.status() != ParticipationStatus.EXITED
                        && participation.status() != ParticipationStatus.REMOVED);
        if (alreadyParticipating) {
            throw new DuplicateGroupParticipationException("member already participates in the group");
        }
        GroupParticipation participation = new GroupParticipation(
                AggregateId.newId(), groupId, role, joinedAt, ParticipationStatus.INVITED, null);
        participations.add(participation);
        markChanged(actorId, joinedAt);
        registerEvent(new MemberJoinedGroup(UUID.randomUUID(), id(), groupId, role, joinedAt));
        return participation;
    }

    public AggregateId tenantId() {
        return tenantId;
    }

    public AggregateId userId() {
        return userId;
    }

    public MemberNumber memberNumber() {
        return memberNumber;
    }

    public MemberStatus status() {
        return status;
    }

    public List<GroupParticipation> participations() {
        return List.copyOf(participations);
    }

    public List<MemberConsent> consents() {
        return List.copyOf(consents);
    }
}
