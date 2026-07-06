package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.MemberCapacity;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.model.UpdatedAt;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MembershipHistoryEventType;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MembershipHistoryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.shared.domain.Money;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

/** Bidirectional mapping between the Savings Group aggregate and its JPA graph. */
@Mapper(config = PersistenceMapperConfiguration.class)
public interface SavingsGroupJpaMapper {

    default SavingsGroup toDomain(SavingsGroupJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        GroupRule rule = new GroupRule(
                JpaMappingSupport.id(entity.getId()),
                new ContributionSchedule(
                        new Money(
                                entity.getContributionAmountPaise(),
                                JpaMappingSupport.currency(entity.getCurrencyCode())),
                        entity.getFrequency(),
                        entity.getStartDate(),
                        entity.getDurationCycles()),
                new MemberCapacity(entity.getMinimumMembers(), entity.getMaximumMembers()),
                entity.getPayoutMethod(),
                entity.isPartialPaymentAllowed());
        List<GroupMember> members = entity.getMembers().stream()
                .map(this::toDomainMember)
                .toList();
        return SavingsGroup.rehydrate(
                new GroupId(JpaMappingSupport.id(entity.getId())),
                JpaMappingSupport.id(entity.getTenantId()),
                new OwnerId(JpaMappingSupport.id(entity.getOrganizer().getId())),
                new GroupCode(entity.getCode()),
                new GroupName(entity.getName()),
                new GroupDescription(entity.getDescription()),
                entity.getType(),
                rule,
                entity.getStatus(),
                members,
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default SavingsGroupJpaEntity toEntity(
            SavingsGroup domain,
            @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        SavingsGroupJpaEntity entity = new SavingsGroupJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.user(domain.organizerId()),
                domain.code().value(),
                domain.name().value(),
                domain.description().value(),
                domain.type(),
                domain.status(),
                domain.rule().contributionSchedule().contributionAmount().minorUnits(),
                domain.rule().contributionSchedule().contributionAmount().currency().getCurrencyCode(),
                domain.rule().contributionSchedule().frequency(),
                domain.rule().contributionSchedule().startDate(),
                domain.rule().contributionSchedule().cycleCount(),
                domain.rule().memberCapacity().minimum(),
                domain.rule().memberCapacity().maximum(),
                domain.rule().payoutMethod(),
                domain.rule().partialPaymentsAllowed());
        entity.synchronizeMembers(toEntityMembers(domain, entity, references));
        return entity;
    }

    default SavingsGroupJpaEntity updateEntity(
            SavingsGroup domain,
            SavingsGroupJpaEntity entity,
            @Context JpaReferenceProvider references) {
        Objects.requireNonNull(domain, "savings group must not be null");
        Objects.requireNonNull(entity, "savings group entity must not be null");
        entity.update(
                domain.tenantId().value(),
                references.user(domain.organizerId()),
                domain.code().value(),
                domain.name().value(),
                domain.description().value(),
                domain.type(),
                domain.status(),
                domain.rule().contributionSchedule().contributionAmount().minorUnits(),
                domain.rule().contributionSchedule().contributionAmount().currency().getCurrencyCode(),
                domain.rule().contributionSchedule().frequency(),
                domain.rule().contributionSchedule().startDate(),
                domain.rule().contributionSchedule().cycleCount(),
                domain.rule().memberCapacity().minimum(),
                domain.rule().memberCapacity().maximum(),
                domain.rule().payoutMethod(),
                domain.rule().partialPaymentsAllowed());
        entity.synchronizeMembers(toEntityMembers(domain, entity, references));
        return entity;
    }

    private GroupMember toDomainMember(GroupMemberJpaEntity entity) {
        UpdatedAt removedAt = entity.getExitedAt() == null ? null : new UpdatedAt(entity.getExitedAt());
        return GroupMember.rehydrate(
                JpaMappingSupport.id(entity.getUser().getId()),
                new CreatedAt(entity.getJoinedAt()),
                removedAt);
    }

    private List<GroupMemberJpaEntity> toEntityMembers(
            SavingsGroup domain,
            SavingsGroupJpaEntity groupEntity,
            JpaReferenceProvider references) {
        List<GroupMemberJpaEntity> entities = new ArrayList<>();
        for (GroupMember member : domain.members()) {
            UUID membershipId = groupEntity.membershipIdForUser(member.memberId().value())
                    .orElseGet(() -> deterministicId(domain.id() + ":" + member.memberId()));
            GroupMemberJpaEntity entity = new GroupMemberJpaEntity(
                    membershipId,
                    domain.tenantId().value(),
                    groupEntity,
                    references.user(member.memberId()),
                    member.memberId().toString().replace("-", ""),
                    member.memberId().equals(domain.organizerId()) ? GroupRole.ORGANIZER : GroupRole.MEMBER,
                    member.isActive() ? ParticipationStatus.ACTIVE : ParticipationStatus.REMOVED,
                    member.joinedAt().value(),
                    member.removedAt() == null ? null : member.removedAt().value());
            entity.replaceHistory(toHistory(domain, member, groupEntity, entity));
            entities.add(entity);
        }
        return entities;
    }

    private List<MembershipHistoryJpaEntity> toHistory(
            SavingsGroup domain,
            GroupMember member,
            SavingsGroupJpaEntity groupEntity,
            GroupMemberJpaEntity membershipEntity) {
        List<MembershipHistoryJpaEntity> history = new ArrayList<>();
        history.add(historyEntry(
                domain,
                member,
                groupEntity,
                membershipEntity,
                MembershipHistoryEventType.JOINED,
                member.joinedAt().value()));
        if (member.removedAt() != null) {
            history.add(historyEntry(
                    domain,
                    member,
                    groupEntity,
                    membershipEntity,
                    MembershipHistoryEventType.REMOVED,
                    member.removedAt().value()));
        }
        return history;
    }

    private MembershipHistoryJpaEntity historyEntry(
            SavingsGroup domain,
            GroupMember member,
            SavingsGroupJpaEntity groupEntity,
            GroupMemberJpaEntity membershipEntity,
            MembershipHistoryEventType eventType,
            java.time.Instant occurredAt) {
        UUID historyId = deterministicId(membershipEntity.getId() + ":" + eventType);
        return new MembershipHistoryJpaEntity(
                historyId,
                domain.tenantId().value(),
                groupEntity,
                membershipEntity,
                member.memberId().value(),
                eventType,
                occurredAt);
    }

    private UUID deterministicId(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}
