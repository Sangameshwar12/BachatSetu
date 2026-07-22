package in.bachatsetu.backend.invitation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserContact;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreviewInvitationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T06:00:00Z");
    private static final String TOKEN = "a".repeat(43);

    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private UserRepository userRepository;
    private TransactionPort transaction;
    private PreviewInvitationApplicationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        userRepository = mock(UserRepository.class);
        transaction = Supplier::get;
        service = new PreviewInvitationApplicationService(
                invitationRepository, groupRepository, userRepository, transaction);
    }

    @Test
    void buildsAPreviewFromAValidToken() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        GroupInvitation invitation = invitationFor(group);
        when(invitationRepository.findByToken(new InvitationToken(TOKEN))).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(invitation.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        when(userRepository.findById(group.ownerId().value())).thenReturn(Optional.of(organizerProfile()));

        InvitationPreviewResult result = service.execute(TOKEN);

        assertThat(result.groupName()).isEqualTo(group.name().value());
        assertThat(result.organizerName()).isEqualTo("Asha Rao");
        assertThat(result.contributionAmountPaise())
                .isEqualTo(group.contributionAmount().value().minorUnits());
        assertThat(result.status()).isEqualTo(group.status().name());
    }

    @Test
    void fallsBackToADefaultOrganizerNameWhenTheProfileIsMissing() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        GroupInvitation invitation = invitationFor(group);
        when(invitationRepository.findByToken(new InvitationToken(TOKEN))).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(invitation.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        when(userRepository.findById(group.ownerId().value())).thenReturn(Optional.empty());

        InvitationPreviewResult result = service.execute(TOKEN);

        assertThat(result.organizerName()).isEqualTo("Group organizer");
    }

    @Test
    void rejectsWhenTheInvitationDoesNotExist() {
        when(invitationRepository.findByToken(new InvitationToken(TOKEN))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TOKEN))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.INVITATION_NOT_FOUND));
    }

    @Test
    void rejectsWhenTheGroupDoesNotExist() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();
        GroupInvitation invitation = GroupInvitation.create(
                AggregateId.newId(), tenantId, groupId, new InvitationCode("AB3D9F2K"),
                new InvitationToken(TOKEN), InvitationType.QR, NOW.plusSeconds(604_800), AggregateId.newId(), NOW);
        when(invitationRepository.findByToken(new InvitationToken(TOKEN))).thenReturn(Optional.of(invitation));
        when(groupRepository.findById(tenantId, new GroupId(groupId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TOKEN))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.GROUP_NOT_FOUND));
    }

    @Test
    void rejectsNullToken() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new PreviewInvitationApplicationService(
                        null, groupRepository, userRepository, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PreviewInvitationApplicationService(
                        invitationRepository, null, userRepository, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PreviewInvitationApplicationService(
                        invitationRepository, groupRepository, null, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PreviewInvitationApplicationService(
                        invitationRepository, groupRepository, userRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private GroupInvitation invitationFor(SavingsGroup group) {
        return GroupInvitation.create(
                AggregateId.newId(), AggregateId.newId(), group.groupId().value(), new InvitationCode("AB3D9F2K"),
                new InvitationToken(TOKEN), InvitationType.QR, NOW.plusSeconds(604_800), group.ownerId().value(), NOW);
    }

    private UserProfile organizerProfile() {
        return UserProfile.register(
                AggregateId.newId(), new PersonName("Asha", "Rao"),
                new UserContact(new Email("asha.rao@example.com"), null),
                PreferredLanguage.ENGLISH, AggregateId.newId(), NOW);
    }
}
