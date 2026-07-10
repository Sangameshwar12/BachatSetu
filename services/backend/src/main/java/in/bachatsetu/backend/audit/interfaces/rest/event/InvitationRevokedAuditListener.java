package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.invitation.domain.event.InvitationRevoked;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Records an {@code INVITATION_REVOKED} audit entry. See {@link InvitationCreatedAuditListener}. */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationRevokedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationRevokedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public InvitationRevokedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onInvitationRevoked(InvitationRevoked event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.groupId(), AuditEventType.INVITATION_REVOKED, "invitation", "GroupInvitation",
                    event.aggregateId(), "INVITATION_REVOKED", "revoked a group invitation", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an invitation-revoked audit entry for {}", event.aggregateId(), exception);
        }
    }
}
