package in.bachatsetu.backend.group.domain.model;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.monthlyRule;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SavingsGroupTest {

    @Test
    void createsInactiveGroupWithOwnerAsFirstMember() {
        AggregateId owner = AggregateId.newId();

        SavingsGroup group = newGroup(owner, 10);

        assertThat(group.groupId().value()).isEqualTo(group.id());
        assertThat(group.tenantId()).isNotNull();
        assertThat(group.ownerId()).isEqualTo(new OwnerId(owner));
        assertThat(group.organizerId()).isEqualTo(owner);
        assertThat(group.code()).isEqualTo(new GroupCode("BS-TEST"));
        assertThat(group.name()).isEqualTo(new GroupName("Bachat Circle"));
        assertThat(group.description()).isEqualTo(new GroupDescription("Monthly community savings"));
        assertThat(group.type()).isEqualTo(GroupType.BHISHI);
        assertThat(group.rule()).isNotNull();
        assertThat(group.contributionAmount()).isEqualTo(ContributionAmount.inPaise(100_000));
        assertThat(group.maximumMembers()).isEqualTo(new MaximumMembers(10));
        assertThat(group.status()).isEqualTo(GroupStatus.INACTIVE);
        assertThat(group.memberCount()).isEqualTo(new MemberCount(1));
        assertThat(group.members()).singleElement().satisfies(member -> {
            assertThat(member.memberId()).isEqualTo(owner);
            assertThat(member.isActive()).isTrue();
        });
        assertThat(group.createdAt()).isEqualTo(new CreatedAt(NOW));
        assertThat(group.updatedAt()).isEqualTo(new UpdatedAt(NOW));
        assertThat(group.version()).isZero();
        assertThat(group.domainEvents()).singleElement().isInstanceOf(SavingsGroupCreated.class);
    }

    @Test
    void activatesAndAddsMember() {
        SavingsGroup group = newGroup(3);
        AggregateId memberId = AggregateId.newId();
        group.pullDomainEvents();

        group.activate(group.organizerId(), NOW.plusSeconds(1));
        GroupMember member = group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));

        assertThat(group.status()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(group.memberCount().value()).isEqualTo(2);
        assertThat(member.memberId()).isEqualTo(memberId);
        assertThat(group.version()).isEqualTo(2);
        assertThat(group.updatedAt().value()).isEqualTo(NOW.plusSeconds(2));
        assertThat(group.domainEvents()).extracting(Object::getClass)
                .containsExactly(GroupActivated.class, MemberJoined.class);
        assertThatThrownBy(() -> group.members().add(member)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMembersUnlessActive() {
        SavingsGroup inactive = newGroup(2);
        SavingsGroup suspended = newGroup(2);
        suspended.activate(suspended.organizerId(), NOW.plusSeconds(1));
        suspended.suspend(suspended.organizerId(), NOW.plusSeconds(2));
        SavingsGroup closed = newGroup(2);
        closed.close(closed.organizerId(), NOW.plusSeconds(1));

        assertThatThrownBy(() -> inactive.joinMember(
                        AggregateId.newId(), inactive.organizerId(), NOW.plusSeconds(1)))
                .isInstanceOf(InvalidGroupStateException.class);
        assertThatThrownBy(() -> suspended.joinMember(
                        AggregateId.newId(), suspended.organizerId(), NOW.plusSeconds(3)))
                .isInstanceOf(InvalidGroupStateException.class);
        assertThatThrownBy(() -> closed.joinMember(
                        AggregateId.newId(), closed.organizerId(), NOW.plusSeconds(2)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void preventsDuplicateMembershipEvenAfterRemoval() {
        SavingsGroup group = activeGroup(3);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));

        assertThatThrownBy(() -> group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(3)))
                .isInstanceOf(DuplicateMemberException.class);

        group.removeMember(memberId, group.organizerId(), NOW.plusSeconds(4));
        assertThatThrownBy(() -> group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(5)))
                .isInstanceOf(DuplicateMemberException.class);
    }

    @Test
    void enforcesMaximumCapacity() {
        SavingsGroup group = activeGroup(2);
        group.joinMember(AggregateId.newId(), group.organizerId(), NOW.plusSeconds(2));

        assertThatThrownBy(() -> group.joinMember(
                        AggregateId.newId(), group.organizerId(), NOW.plusSeconds(3)))
                .isInstanceOf(GroupCapacityExceededException.class);
    }

    @Test
    void removesNonOwnerAndRetainsMembershipHistory() {
        SavingsGroup group = activeGroup(3);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));
        group.pullDomainEvents();

        group.removeMember(memberId, group.organizerId(), NOW.plusSeconds(3));

        assertThat(group.memberCount().value()).isEqualTo(1);
        assertThat(group.members()).filteredOn(member -> member.memberId().equals(memberId))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.isActive()).isFalse();
                    assertThat(member.removedAt()).isEqualTo(new UpdatedAt(NOW.plusSeconds(3)));
                });
        assertThat(group.domainEvents()).singleElement().isInstanceOf(MemberRemoved.class);
    }

    @Test
    void protectsOwnerAndRejectsUnknownOrAlreadyRemovedMember() {
        SavingsGroup group = activeGroup(3);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));
        group.removeMember(memberId, group.organizerId(), NOW.plusSeconds(3));

        assertThatThrownBy(() -> group.removeMember(
                        group.organizerId(), group.organizerId(), NOW.plusSeconds(4)))
                .isInstanceOf(OwnerRemovalNotAllowedException.class);
        assertThatThrownBy(() -> group.removeMember(
                        AggregateId.newId(), group.organizerId(), NOW.plusSeconds(4)))
                .isInstanceOf(InvalidGroupStateException.class);
        assertThatThrownBy(() -> group.removeMember(
                        memberId, group.organizerId(), NOW.plusSeconds(4)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void closedGroupCannotRemoveMember() {
        SavingsGroup group = activeGroup(3);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, group.organizerId(), NOW.plusSeconds(2));
        group.close(group.organizerId(), NOW.plusSeconds(3));

        assertThatThrownBy(() -> group.removeMember(
                        memberId, group.organizerId(), NOW.plusSeconds(4)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void followsAllowedLifecycleTransitionsAndEmitsEvents() {
        SavingsGroup group = newGroup(3);
        group.pullDomainEvents();

        group.activate(group.organizerId(), NOW.plusSeconds(1));
        group.suspend(group.organizerId(), NOW.plusSeconds(2));
        group.activate(group.organizerId(), NOW.plusSeconds(3));
        group.close(group.organizerId(), NOW.plusSeconds(4));

        assertThat(group.status()).isEqualTo(GroupStatus.CLOSED);
        assertThat(group.domainEvents()).extracting(Object::getClass)
                .containsExactly(
                        GroupActivated.class,
                        GroupSuspended.class,
                        GroupActivated.class,
                        GroupClosed.class);
        assertThatThrownBy(() -> group.activate(group.organizerId(), NOW.plusSeconds(5)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void inactiveGroupCanCloseButCannotSuspend() {
        SavingsGroup closable = newGroup(2);
        SavingsGroup notSuspendable = newGroup(2);

        closable.close(closable.organizerId(), NOW.plusSeconds(1));

        assertThat(closable.status()).isEqualTo(GroupStatus.CLOSED);
        assertThatThrownBy(() -> notSuspendable.suspend(
                        notSuspendable.organizerId(), NOW.plusSeconds(1)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void activeGroupCannotActivateAgainAndSuspendedGroupCannotClose() {
        SavingsGroup active = activeGroup(2);
        SavingsGroup suspended = activeGroup(2);
        suspended.suspend(suspended.organizerId(), NOW.plusSeconds(2));

        assertThatThrownBy(() -> active.activate(active.organizerId(), NOW.plusSeconds(2)))
                .isInstanceOf(InvalidGroupStateException.class);
        assertThatThrownBy(() -> suspended.close(suspended.organizerId(), NOW.plusSeconds(3)))
                .isInstanceOf(InvalidGroupStateException.class);
    }

    @Test
    void rehydratesWithoutEventsAndPreservesState() {
        AggregateId owner = AggregateId.newId();
        AggregateId member = AggregateId.newId();
        GroupId groupId = GroupId.newId();
        AuditInfo audit = new AuditInfo(NOW, owner, NOW.plusSeconds(4), owner);
        List<GroupMember> members = new ArrayList<>(List.of(
                GroupMember.join(owner, new CreatedAt(NOW)),
                GroupMember.join(member, new CreatedAt(NOW.plusSeconds(2)))));

        SavingsGroup group = SavingsGroup.rehydrate(
                groupId,
                AggregateId.newId(),
                new OwnerId(owner),
                new GroupCode("BS-RESTORED"),
                new GroupName("Restored Group"),
                new GroupDescription("Restored description"),
                GroupType.BHISHI,
                monthlyRule(4),
                GroupStatus.SUSPENDED,
                members,
                audit,
                7);
        members.clear();

        assertThat(group.groupId()).isEqualTo(groupId);
        assertThat(group.status()).isEqualTo(GroupStatus.SUSPENDED);
        assertThat(group.memberCount().value()).isEqualTo(2);
        assertThat(group.members()).hasSize(2);
        assertThat(group.auditInfo()).isEqualTo(audit);
        assertThat(group.version()).isEqualTo(7);
        assertThat(group.domainEvents()).isEmpty();
    }

    @Test
    void legacyConstructorRehydratesWithoutEvents() {
        AggregateId owner = AggregateId.newId();
        SavingsGroup group = new SavingsGroup(
                AggregateId.newId(),
                AggregateId.newId(),
                owner,
                new GroupCode("BS-LEGACY"),
                new GroupName("Legacy Group"),
                GroupType.BHISHI,
                monthlyRule(2),
                GroupStatus.ACTIVE,
                AuditInfo.createdBy(owner, NOW),
                3);

        assertThat(group.description()).isEqualTo(GroupDescription.empty());
        assertThat(group.memberCount().value()).isEqualTo(1);
        assertThat(group.version()).isEqualTo(3);
        assertThat(group.domainEvents()).isEmpty();
    }

    @Test
    void rejectsChangesOlderThanPreviousAggregateUpdate() {
        SavingsGroup group = activeGroup(2);

        assertThatThrownBy(() -> group.close(group.organizerId(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(group.status()).isEqualTo(GroupStatus.ACTIVE);
    }

    private SavingsGroup activeGroup(int maximumMembers) {
        SavingsGroup group = newGroup(maximumMembers);
        group.activate(group.organizerId(), NOW.plusSeconds(1));
        return group;
    }
}
