package in.bachatsetu.backend.email.interfaces.rest.event;

import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.domain.event.InvitationRevoked;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Sends an {@code INVITATION_REVOKED} confirmation email to the organizer, mirroring {@link
 * InvitationCreatedEmailListener} exactly — same recipient-resolution reasoning, same
 * best-effort semantics.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationRevokedEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationRevokedEmailListener.class);

    private final SendEmailUseCase sendEmail;
    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final UserRepository userRepository;

    public InvitationRevokedEmailListener(
            SendEmailUseCase sendEmail,
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            UserRepository userRepository) {
        this.sendEmail = Objects.requireNonNull(sendEmail, "sendEmail must not be null");
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @EventListener
    public void onInvitationRevoked(InvitationRevoked event) {
        try {
            GroupInvitation invitation = invitationRepository.findById(event.aggregateId()).orElse(null);
            if (invitation == null) {
                return;
            }
            SavingsGroup group = groupRepository.findById(invitation.tenantId(), new GroupId(invitation.groupId()))
                    .orElse(null);
            User organizer = userRepository.findById(new UserId(invitation.auditInfo().createdBy().value()))
                    .orElse(null);
            if (group == null || organizer == null) {
                return;
            }
            sendEmail.execute(new SendEmailCommand(
                    new EmailAddress(organizer.email().value()),
                    EmailTemplateCategory.INVITATION_REVOKED,
                    Map.of("groupName", group.name().value())));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to send an invitation-revoked email for invitation {}", event.aggregateId(), exception);
        }
    }
}
