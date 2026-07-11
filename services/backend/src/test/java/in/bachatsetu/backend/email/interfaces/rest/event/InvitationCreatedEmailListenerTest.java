package in.bachatsetu.backend.email.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.domain.event.InvitationCreated;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvitationCreatedEmailListenerTest {

    private static final AggregateId INVITATION_ID = AggregateId.newId();
    private static final AggregateId GROUP_ID = AggregateId.newId();
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId ORGANIZER_ID = AggregateId.newId();

    private SendEmailUseCase sendEmail;
    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private UserRepository userRepository;
    private InvitationCreatedEmailListener listener;

    @BeforeEach
    void setUp() {
        sendEmail = mock(SendEmailUseCase.class);
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        userRepository = mock(UserRepository.class);
        listener = new InvitationCreatedEmailListener(sendEmail, invitationRepository, groupRepository, userRepository);
    }

    @Test
    void sendsAnInvitationEmailToTheOrganizerWithTheGroupNameAndCode() {
        GroupInvitation invitation = mock(GroupInvitation.class);
        when(invitation.tenantId()).thenReturn(TENANT_ID);
        when(invitation.groupId()).thenReturn(GROUP_ID);
        when(invitation.code()).thenReturn(new InvitationCode("ABC123"));
        when(invitation.auditInfo()).thenReturn(AuditInfo.createdBy(ORGANIZER_ID, Instant.now()));
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(invitation));

        SavingsGroup group = mock(SavingsGroup.class);
        when(group.name()).thenReturn(new GroupName("Diwali Bachat"));
        when(groupRepository.findById(eq(TENANT_ID), any())).thenReturn(Optional.of(group));

        User organizer = mock(User.class);
        when(organizer.email()).thenReturn(new Email("organizer@example.com"));
        when(userRepository.findById(any())).thenReturn(Optional.of(organizer));

        InvitationCreated event = new InvitationCreated(UUID.randomUUID(), INVITATION_ID, GROUP_ID, Instant.now());
        listener.onInvitationCreated(event);

        ArgumentCaptor<SendEmailCommand> captor = ArgumentCaptor.forClass(SendEmailCommand.class);
        verify(sendEmail).execute(captor.capture());
        assertThat(captor.getValue().to().value()).isEqualTo("organizer@example.com");
        assertThat(captor.getValue().category()).isEqualTo(EmailTemplateCategory.INVITATION);
        assertThat(captor.getValue().variables()).containsEntry("groupName", "Diwali Bachat");
        assertThat(captor.getValue().variables()).containsEntry("invitationCode", "ABC123");
    }

    @Test
    void doesNothingWhenTheInvitationCannotBeFound() {
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.empty());

        listener.onInvitationCreated(
                new InvitationCreated(UUID.randomUUID(), INVITATION_ID, GROUP_ID, Instant.now()));

        verifyNoInteractions(sendEmail);
    }

    @Test
    void swallowsAnEmailSendFailureWithoutPropagating() {
        GroupInvitation invitation = mock(GroupInvitation.class);
        when(invitation.tenantId()).thenReturn(TENANT_ID);
        when(invitation.groupId()).thenReturn(GROUP_ID);
        when(invitation.code()).thenReturn(new InvitationCode("ABC123"));
        when(invitation.auditInfo()).thenReturn(AuditInfo.createdBy(ORGANIZER_ID, Instant.now()));
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(invitation));
        SavingsGroup group = mock(SavingsGroup.class);
        when(group.name()).thenReturn(new GroupName("Diwali Bachat"));
        when(groupRepository.findById(eq(TENANT_ID), any())).thenReturn(Optional.of(group));
        User organizer = mock(User.class);
        when(organizer.email()).thenReturn(new Email("organizer@example.com"));
        when(userRepository.findById(any())).thenReturn(Optional.of(organizer));
        when(sendEmail.execute(any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> listener.onInvitationCreated(
                new InvitationCreated(UUID.randomUUID(), INVITATION_ID, GROUP_ID, Instant.now())))
                .doesNotThrowAnyException();
    }
}
