package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.support.domain.event.SupportTicketAssigned;
import in.bachatsetu.backend.support.domain.event.SupportTicketClosed;
import in.bachatsetu.backend.support.domain.event.SupportTicketCreated;
import in.bachatsetu.backend.support.domain.event.SupportTicketResolved;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records audit entries for the SupportTicket lifecycle. Reacts to the Support module's own domain events
 * rather than being called directly by its application services, for the same cycle-avoidance reason
 * documented on {@link InvitationCreatedAuditListener}.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SupportAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public SupportAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onTicketCreated(SupportTicketCreated event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    event.tenantId(), null, AuditEventType.SUPPORT_TICKET_CREATED, "support", "SupportTicket",
                    event.aggregateId(), "SUPPORT_TICKET_CREATED",
                    "raised a " + event.priority() + " priority " + event.category() + " ticket", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a ticket-created audit entry for {}", event.aggregateId(), exception);
        }
    }

    @EventListener
    public void onTicketAssigned(SupportTicketAssigned event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.assignedTo(), AuditEventType.SUPPORT_TICKET_ASSIGNED, "support", "SupportTicket",
                    event.aggregateId(), "SUPPORT_TICKET_ASSIGNED", "assigned a support ticket", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a ticket-assigned audit entry for {}", event.aggregateId(), exception);
        }
    }

    @EventListener
    public void onTicketResolved(SupportTicketResolved event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, null, AuditEventType.SUPPORT_TICKET_RESOLVED, "support", "SupportTicket",
                    event.aggregateId(), "SUPPORT_TICKET_RESOLVED", "resolved a support ticket", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a ticket-resolved audit entry for {}", event.aggregateId(), exception);
        }
    }

    @EventListener
    public void onTicketClosed(SupportTicketClosed event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, null, AuditEventType.SUPPORT_TICKET_CLOSED, "support", "SupportTicket",
                    event.aggregateId(), "SUPPORT_TICKET_CLOSED", "closed a support ticket", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a ticket-closed audit entry for {}", event.aggregateId(), exception);
        }
    }
}
