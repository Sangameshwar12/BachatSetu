package in.bachatsetu.backend.group.application.mapper;

import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.time.Instant;
import java.util.Objects;

/** Maps rich domain objects to immutable application query models. */
public final class SavingsGroupApplicationMapper {

    public SavingsGroupResult toResult(SavingsGroup group) {
        Objects.requireNonNull(group, "savings group must not be null");
        return new SavingsGroupResult(
                group.groupId().value().value(),
                group.tenantId().value(),
                group.ownerId().value().value(),
                group.code().value(),
                group.name().value(),
                group.description().value(),
                group.type().name(),
                group.status().name(),
                group.contributionAmount().value().minorUnits(),
                group.contributionAmount().value().currency().getCurrencyCode(),
                group.maximumMembers().value(),
                group.memberCount().value(),
                group.createdAt().value(),
                group.updatedAt().value(),
                group.version(),
                group.members().stream().map(this::toMemberResult).toList());
    }

    public SavingsGroupSummary toSummary(SavingsGroup group) {
        Objects.requireNonNull(group, "savings group must not be null");
        return new SavingsGroupSummary(
                group.groupId().value().value(),
                group.code().value(),
                group.name().value(),
                group.status().name(),
                group.contributionAmount().value().minorUnits(),
                group.contributionAmount().value().currency().getCurrencyCode(),
                group.maximumMembers().value(),
                group.memberCount().value());
    }

    public GroupMemberResult toMemberResult(GroupMember member) {
        Objects.requireNonNull(member, "group member must not be null");
        Instant removedAt = member.removedAt() == null ? null : member.removedAt().value();
        return new GroupMemberResult(
                member.memberId().value(), member.joinedAt().value(), removedAt, member.isActive());
    }
}
