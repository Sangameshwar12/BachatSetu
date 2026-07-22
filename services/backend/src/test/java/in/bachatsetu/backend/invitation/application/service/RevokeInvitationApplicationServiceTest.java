package in.bachatsetu.backend.invitation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.command.RevokeInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RevokeInvitationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T06:00:00Z");
    private static final AggregateId TENANT_ID = AggregateId.newId();

    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private DomainEventPublisherPort eventPublisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private RevokeInvitationApplicationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW;
        transaction = Supplier::get;
        service = new RevokeInvitationApplicationService(
                invitationRepository, groupRepository, eventPublisher, clock, transaction);
    }

    @Test
    void revokesTheActiveInvitation() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        GroupInvitation invitation = activeInvitation(group);
        when(invitationRepository.findActiveByGroup(TENANT_ID, group.groupId().value()))
                .thenReturn(Optional.of(invitation));

        service.execute(new RevokeInvitationCommand(TENANT_ID, group.groupId().value(), group.ownerId().value()));

        assertThat(invitation.status()).isEqualTo(InvitationStatus.CANCELLED);
        verify(invitationRepository).save(invitation);
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsANonOwnerActor() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        AggregateId stranger = AggregateId.newId();

        assertThatThrownBy(() -> service.execute(
                        new RevokeInvitationCommand(TENANT_ID, group.groupId().value(), stranger)))
                .isInstanceOf(GroupAccessDeniedException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void rejectsWhenGroupDoesNotExist() {
        AggregateId groupId = AggregateId.newId();
        when(groupRepository.findById(TENANT_ID, new GroupId(groupId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new RevokeInvitationCommand(TENANT_ID, groupId, AggregateId.newId())))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.GROUP_NOT_FOUND));
    }

    @Test
    void rejectsWhenNoActiveInvitationExists() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        when(invitationRepository.findActiveByGroup(TENANT_ID, group.groupId().value())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new RevokeInvitationCommand(TENANT_ID, group.groupId().value(), group.ownerId().value())))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.NO_ACTIVE_INVITATION));
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new RevokeInvitationApplicationService(
                        null, groupRepository, eventPublisher, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RevokeInvitationApplicationService(
                        invitationRepository, null, eventPublisher, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RevokeInvitationApplicationService(
                        invitationRepository, groupRepository, null, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RevokeInvitationApplicationService(
                        invitationRepository, groupRepository, eventPublisher, null, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RevokeInvitationApplicationService(
                        invitationRepository, groupRepository, eventPublisher, clock, null))
                .isInstanceOf(NullPointerException.class);
    }

    private GroupInvitation activeInvitation(SavingsGroup group) {
        return GroupInvitation.create(
                AggregateId.newId(), TENANT_ID, group.groupId().value(), new InvitationCode("AB3D9F2K"),
                new InvitationToken("a".repeat(43)), InvitationType.CODE, NOW.plusSeconds(604_800),
                group.ownerId().value(), NOW);
    }
}
