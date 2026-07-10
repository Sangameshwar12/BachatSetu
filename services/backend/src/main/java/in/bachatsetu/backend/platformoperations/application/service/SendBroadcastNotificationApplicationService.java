package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.platformoperations.application.command.SendBroadcastNotificationCommand;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;
import in.bachatsetu.backend.platformoperations.application.usecase.SendBroadcastNotificationUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastRecipient;
import in.bachatsetu.backend.platformoperations.domain.port.BroadcastRecipientRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sends a broadcast notification by resolving the target audience and then calling the existing, unmodified
 * {@link CreateNotificationUseCase} once per recipient — this deliberately reuses the Notification module's
 * own domain invariants and delivery pipeline rather than duplicating any notification logic. Each recipient
 * is attempted independently: one recipient's failure does not stop the rest of the broadcast.
 *
 * <p>Records a {@code BROADCAST_NOTIFICATION_SENT} audit entry directly rather than through an Audit event
 * listener: this module already depends on Notification (right above), which itself depends on Audit, so a
 * dependency from Audit back onto this module would form a module cycle. Best-effort: a failure to record
 * the audit entry must never affect an already-sent broadcast.
 */
public final class SendBroadcastNotificationApplicationService implements SendBroadcastNotificationUseCase {

    private final BroadcastRecipientRepository recipientRepository;
    private final CreateNotificationUseCase createNotification;
    private final CreateAuditEntryUseCase createAuditEntry;
    private final TransactionPort transaction;

    public SendBroadcastNotificationApplicationService(
            BroadcastRecipientRepository recipientRepository,
            CreateNotificationUseCase createNotification,
            CreateAuditEntryUseCase createAuditEntry,
            TransactionPort transaction) {
        this.recipientRepository = Objects.requireNonNull(recipientRepository, "recipientRepository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "createNotification must not be null");
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "createAuditEntry must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public BroadcastResult execute(SendBroadcastNotificationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        List<BroadcastRecipient> recipients = transaction.execute(
                () -> recipientRepository.resolve(command.scope(), command.tenantId()));
        int sent = 0;
        int failed = 0;
        for (BroadcastRecipient recipient : recipients) {
            try {
                createNotification.execute(new CreateNotificationCommand(
                        recipient.tenantId(), recipient.userId(), recipient.userId().value().toString(),
                        NotificationChannel.PUSH, NotificationCategory.PLATFORM_ANNOUNCEMENT,
                        Map.of("title", command.title(), "body", command.message()), command.actorId()));
                sent++;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        BroadcastResult result = new BroadcastResult(recipients.size(), sent, failed);
        auditBroadcastSent(command, result);
        return result;
    }

    private void auditBroadcastSent(SendBroadcastNotificationCommand command, BroadcastResult result) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    command.tenantId(), command.actorId(), AuditEventType.BROADCAST_NOTIFICATION_SENT,
                    "platformoperations", "Broadcast", null, "BROADCAST_NOTIFICATION_SENT",
                    "sent a " + command.scope() + " broadcast notification to " + result.sentCount()
                            + " of " + result.recipientCount() + " recipients",
                    null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-sent broadcast.
        }
    }
}
