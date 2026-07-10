package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.invitation.domain.event.InvitationCreated;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records an {@code INVITATION_CREATED} audit entry. See {@link LoginAuditListener} for why this
 * reacts to the pre-existing {@link InvitationCreated} domain event rather than being called
 * directly by {@code CreateInvitationApplicationService}: Audit's own REST layer already depends on
 * {@code auth} (for {@code CurrentUserProvider}), so a direct call the other way would form a cycle.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationCreatedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationCreatedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public InvitationCreatedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onInvitationCreated(InvitationCreated event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.groupId(), AuditEventType.INVITATION_CREATED, "invitation", "GroupInvitation",
                    event.aggregateId(), "INVITATION_CREATED", "generated a group invitation", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an invitation-created audit entry for {}", event.aggregateId(), exception);
        }
    }
}
