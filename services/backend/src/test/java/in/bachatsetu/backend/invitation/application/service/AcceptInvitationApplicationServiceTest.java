package in.bachatsetu.backend.invitation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.exception.DuplicateMemberException;
import in.bachatsetu.backend.invitation.application.command.AcceptInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcceptInvitationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId ACTOR_ID = AggregateId.newId();

    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private DomainEventPublisherPort eventPublisher;
    private in.bachatsetu.backend.group.application.port.DomainEventPublisherPort groupEventPublisher;
    private AcceptInvitationApplicationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        groupEventPublisher = mock(in.bachatsetu.backend.group.application.port.DomainEventPublisherPort.class);
        ClockPort clock = () -> NOW;
        TransactionPort transaction = java.util.function.Supplier::get;
        service = new AcceptInvitationApplicationService(
                invitationRepository, groupRepository, eventPublisher, groupEventPublisher, clock, transaction);
    }

    @Test
    void acceptsByCodeAndJoinsTheGroup() {
        SavingsGroup group = activeGroup(10);
        GroupInvitation invitation = activeInvitation(group);
        when(invitationRepository.findByCode(TENANT_ID, invitation.code())).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));

        InvitationAcceptedResult result = service.execute(new AcceptInvitationCommand(
                TENANT_ID, invitation.code().value(), null, InvitationType.CODE, ACTOR_ID));

        assertThat(result.memberId()).isEqualTo(ACTOR_ID);
        assertThat(result.groupId()).isEqualTo(invitation.groupId());
        verify(invitationRepository).save(invitation);
        verify(groupRepository).save(group);
        verify(eventPublisher).publish(any());
        verify(groupEventPublisher).publish(any());
    }

    @Test
    void acceptsByTokenWhenNoCodeIsSupplied() {
        SavingsGroup group = activeGroup(10);
        GroupInvitation invitation = activeInvitation(group);
        when(invitationRepository.findByToken(invitation.token())).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));

        service.execute(new AcceptInvitationCommand(TENANT_ID, null, invitation.token().value(), InvitationType.QR, ACTOR_ID));

        verify(invitationRepository).save(invitation);
    }

    @Test
    void rejectsWhenInvitationDoesNotExist() {
        when(invitationRepository.findByCode(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new AcceptInvitationCommand(TENANT_ID, "AB3D9F2K", null, InvitationType.CODE, ACTOR_ID)))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.INVITATION_NOT_FOUND));
    }

    @Test
    void doesNotConsumeTheInvitationWhenTheJoinerIsAlreadyAMember() {
        SavingsGroup group = activeGroup(10);
        GroupInvitation invitation = activeInvitation(group);
        when(invitationRepository.findByCode(TENANT_ID, invitation.code())).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.execute(new AcceptInvitationCommand(
                        TENANT_ID, invitation.code().value(), null, InvitationType.CODE, group.ownerId().value())))
                .isInstanceOf(DuplicateMemberException.class);
    }

    private SavingsGroup activeGroup(int maximumMembers) {
        SavingsGroup group = GroupDomainFixtures.newGroup(maximumMembers);
        group.activate(group.ownerId().value(), GroupDomainFixtures.NOW);
        return group;
    }

    private GroupInvitation activeInvitation(SavingsGroup group) {
        return GroupInvitation.create(
                AggregateId.newId(), TENANT_ID, group.groupId().value(), new InvitationCode("AB3D9F2K"),
                new InvitationToken("a".repeat(43)), InvitationType.CODE, NOW.plusSeconds(604_800),
                group.ownerId().value(), NOW);
    }
}
