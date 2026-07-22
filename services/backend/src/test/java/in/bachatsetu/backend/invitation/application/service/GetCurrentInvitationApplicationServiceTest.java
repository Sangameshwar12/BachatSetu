package in.bachatsetu.backend.invitation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCurrentInvitationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T06:00:00Z");
    private static final AggregateId TENANT_ID = AggregateId.newId();

    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private TransactionPort transaction;
    private GetCurrentInvitationApplicationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        transaction = Supplier::get;
        service = new GetCurrentInvitationApplicationService(invitationRepository, groupRepository, transaction);
    }

    @Test
    void returnsTheActiveInvitationForTheOwner() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        GroupInvitation invitation = activeInvitation(group);
        when(invitationRepository.findActiveByGroup(TENANT_ID, group.groupId().value()))
                .thenReturn(Optional.of(invitation));

        InvitationResult result = service.execute(TENANT_ID, group.groupId().value(), group.ownerId().value());

        assertThat(result.code()).isEqualTo(invitation.code().value());
        assertThat(result.groupId()).isEqualTo(group.groupId().value());
    }

    @Test
    void rejectsANonOwnerActor() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.execute(TENANT_ID, group.groupId().value(), AggregateId.newId()))
                .isInstanceOf(GroupAccessDeniedException.class);
    }

    @Test
    void rejectsWhenGroupDoesNotExist() {
        AggregateId groupId = AggregateId.newId();
        when(groupRepository.findById(TENANT_ID, new GroupId(groupId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, groupId, AggregateId.newId()))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.GROUP_NOT_FOUND));
    }

    @Test
    void rejectsWhenNoActiveInvitationExists() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        when(invitationRepository.findActiveByGroup(TENANT_ID, group.groupId().value())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, group.groupId().value(), group.ownerId().value()))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.NO_ACTIVE_INVITATION));
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> service.execute(null, AggregateId.newId(), AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.execute(TENANT_ID, null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.execute(TENANT_ID, AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new GetCurrentInvitationApplicationService(null, groupRepository, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCurrentInvitationApplicationService(invitationRepository, null, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCurrentInvitationApplicationService(invitationRepository, groupRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private GroupInvitation activeInvitation(SavingsGroup group) {
        return GroupInvitation.create(
                AggregateId.newId(), TENANT_ID, group.groupId().value(), new InvitationCode("AB3D9F2K"),
                new InvitationToken("a".repeat(43)), InvitationType.CODE, NOW.plusSeconds(604_800),
                group.ownerId().value(), NOW);
    }
}
