package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.invitation.domain.event.InvitationAccepted;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code GROUP_JOINED}, {@code QR_JOINED}, or {@code LINK_JOINED} audit entry depending
 * on which channel the joiner used to accept the invitation. See {@link InvitationCreatedAuditListener}
 * for why this reacts to the domain event rather than being called directly.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationAcceptedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationAcceptedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public InvitationAcceptedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onInvitationAccepted(InvitationAccepted event) {
        try {
            AuditEventType eventType = switch (event.channel()) {
                case QR -> AuditEventType.QR_JOINED;
                case LINK -> AuditEventType.LINK_JOINED;
                case CODE -> AuditEventType.GROUP_JOINED;
            };
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.acceptedBy(), eventType, "invitation", "GroupInvitation", event.aggregateId(),
                    eventType.name(), "joined a group via " + event.channel(), null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an invitation-accepted audit entry for {}", event.aggregateId(), exception);
        }
    }
}
