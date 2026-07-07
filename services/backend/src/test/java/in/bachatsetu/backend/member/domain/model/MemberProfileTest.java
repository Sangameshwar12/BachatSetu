package in.bachatsetu.backend.member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.member.domain.event.MemberCreated;
import in.bachatsetu.backend.member.domain.event.MemberJoinedGroup;
import in.bachatsetu.backend.member.domain.event.MemberStatusChanged;
import in.bachatsetu.backend.member.domain.exception.DuplicateGroupParticipationException;
import in.bachatsetu.backend.member.domain.exception.InvalidMembershipStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberProfileTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    @Test
    void createsAnInvitedMemberAndEmitsMemberCreated() {
        AggregateId memberId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();

        MemberProfile member = MemberProfile.create(
                memberId, tenantId, userId, new MemberNumber("MB-1A2B3C4D5E6F7A8B"), actorId, NOW);

        assertThat(member.status()).isEqualTo(MemberStatus.INVITED);
        assertThat(member.participations()).isEmpty();
        assertThat(member.domainEvents()).singleElement().isInstanceOf(MemberCreated.class);
    }

    @Test
    void joinsAGroupAndRejectsDuplicateParticipation() {
        MemberProfile member = invitedMember();
        AggregateId groupId = AggregateId.newId();
        member.joinGroup(groupId, GroupRole.MEMBER, member.userId(), NOW.plusSeconds(1));

        assertThat(member.participations()).singleElement()
                .satisfies(participation -> assertThat(participation.groupId()).isEqualTo(groupId));
        assertThat(member.domainEvents()).filteredOn(MemberJoinedGroup.class::isInstance).hasSize(1);
        assertThatThrownBy(() -> member.joinGroup(groupId, GroupRole.MEMBER, member.userId(), NOW.plusSeconds(2)))
                .isInstanceOf(DuplicateGroupParticipationException.class);
    }

    @Test
    void changesStatusAndEmitsMemberStatusChanged() {
        MemberProfile member = invitedMember();
        member.pullDomainEvents();

        member.changeStatus(MemberStatus.ACTIVE, member.userId(), NOW.plusSeconds(1));

        assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.version()).isEqualTo(1);
        assertThat(member.domainEvents()).singleElement().satisfies(event -> {
            assertThat(event).isInstanceOf(MemberStatusChanged.class);
            MemberStatusChanged statusChanged = (MemberStatusChanged) event;
            assertThat(statusChanged.previousStatus()).isEqualTo(MemberStatus.INVITED);
            assertThat(statusChanged.newStatus()).isEqualTo(MemberStatus.ACTIVE);
        });
    }

    @Test
    void rejectsTransitioningToTheSameStatus() {
        MemberProfile member = invitedMember();

        assertThatThrownBy(() -> member.changeStatus(MemberStatus.INVITED, member.userId(), NOW.plusSeconds(1)))
                .isInstanceOf(InvalidMembershipStateException.class);
    }

    @Test
    void rejectsTransitioningOutOfATerminalStatus() {
        MemberProfile exited = invitedMember();
        exited.changeStatus(MemberStatus.EXITED, exited.userId(), NOW.plusSeconds(1));
        MemberProfile removed = invitedMember();
        removed.changeStatus(MemberStatus.REMOVED, removed.userId(), NOW.plusSeconds(1));

        assertThatThrownBy(() -> exited.changeStatus(MemberStatus.ACTIVE, exited.userId(), NOW.plusSeconds(2)))
                .isInstanceOf(InvalidMembershipStateException.class);
        assertThatThrownBy(() -> removed.changeStatus(MemberStatus.ACTIVE, removed.userId(), NOW.plusSeconds(2)))
                .isInstanceOf(InvalidMembershipStateException.class);
    }

    @Test
    void rejectsNullChangeStatusArguments() {
        MemberProfile member = invitedMember();

        assertThatThrownBy(() -> member.changeStatus(null, member.userId(), NOW.plusSeconds(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> member.changeStatus(MemberStatus.ACTIVE, member.userId(), null))
                .isInstanceOf(NullPointerException.class);
    }

    private MemberProfile invitedMember() {
        return MemberProfile.create(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new MemberNumber("MB-1A2B3C4D5E6F7A8B"), AggregateId.newId(), NOW);
    }
}
