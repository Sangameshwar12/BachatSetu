package in.bachatsetu.backend.group.domain.service;

import in.bachatsetu.backend.group.domain.exception.DuplicateMemberException;
import in.bachatsetu.backend.group.domain.exception.GroupCapacityExceededException;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.group.domain.model.ContributionAmount;
import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.MaximumMembers;
import in.bachatsetu.backend.group.domain.model.MemberCount;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Stateless cross-object validation for group construction and reconstruction. */
public final class GroupValidationService {

    public void validateCreation(
            OwnerId ownerId,
            ContributionAmount contributionAmount,
            MaximumMembers maximumMembers,
            GroupRule rule) {
        Objects.requireNonNull(ownerId, "owner id must not be null");
        Objects.requireNonNull(contributionAmount, "contribution amount must not be null");
        Objects.requireNonNull(maximumMembers, "maximum members must not be null");
        Objects.requireNonNull(rule, "group rule must not be null");
        if (rule.contributionSchedule().frequency() != ContributionFrequency.MONTHLY) {
            throw new InvalidGroupStateException("savings group contributions must be monthly");
        }
        if (!rule.contributionSchedule().contributionAmount().equals(contributionAmount.value())) {
            throw new IllegalArgumentException("group rule contribution amount is inconsistent");
        }
        if (rule.memberCapacity().maximum() != maximumMembers.value()) {
            throw new IllegalArgumentException("group rule maximum members is inconsistent");
        }
    }

    public void validateMemberships(
            OwnerId ownerId,
            MaximumMembers maximumMembers,
            List<GroupMember> members) {
        Objects.requireNonNull(ownerId, "owner id must not be null");
        Objects.requireNonNull(maximumMembers, "maximum members must not be null");
        Objects.requireNonNull(members, "members must not be null");
        Set<AggregateId> memberIds = new HashSet<>();
        int activeMembers = 0;
        boolean activeOwnerPresent = false;
        for (GroupMember member : members) {
            Objects.requireNonNull(member, "member must not be null");
            if (!memberIds.add(member.memberId())) {
                throw new DuplicateMemberException("membership history contains a duplicate member");
            }
            if (member.isActive()) {
                activeMembers++;
                activeOwnerPresent |= ownerId.value().equals(member.memberId());
            }
        }
        if (!activeOwnerPresent) {
            throw new InvalidGroupStateException("group owner must be an active member");
        }
        if (!maximumMembers.accommodates(new MemberCount(activeMembers))) {
            throw new GroupCapacityExceededException("membership exceeds maximum capacity");
        }
    }
}
