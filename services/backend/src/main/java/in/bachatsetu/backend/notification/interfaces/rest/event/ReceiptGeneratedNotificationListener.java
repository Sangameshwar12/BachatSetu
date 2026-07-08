package in.bachatsetu.backend.notification.interfaces.rest.event;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.receipt.domain.event.ReceiptGenerated;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies a receipt's member once the receipt has been generated.
 *
 * <p>Reacts to the pre-existing {@link ReceiptGenerated} domain event rather than being called directly by
 * any Receipt application service, so the Receipt module holds no compile-time dependency on Notification.
 *
 * <p>Listens with {@link TransactionPhase#AFTER_COMMIT} so a notification is only attempted once the
 * generating transaction has durably committed. A failure while notifying is logged and swallowed rather
 * than rethrown: receipt generation is the primary business operation, and a best-effort notification must
 * never appear to fail it.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReceiptGeneratedNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptGeneratedNotificationListener.class);

    private final ReceiptRepository receiptRepository;
    private final CreateNotificationUseCase createNotification;

    public ReceiptGeneratedNotificationListener(
            ReceiptRepository receiptRepository, CreateNotificationUseCase createNotification) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receipt repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReceiptGenerated(ReceiptGenerated event) {
        try {
            notifyReceiptGenerated(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a receipt-generated notification for receipt {}", event.aggregateId(), exception);
        }
    }

    private void notifyReceiptGenerated(ReceiptGenerated event) {
        Receipt receipt = receiptRepository.findById(event.aggregateId()).orElse(null);
        if (receipt == null) {
            return;
        }
        createNotification.execute(new CreateNotificationCommand(
                receipt.tenantId(),
                receipt.memberId(),
                receipt.memberId().value().toString(),
                NotificationChannel.PUSH,
                NotificationCategory.RECEIPT,
                Map.of("title", "Receipt Available", "body", "Your payment receipt is ready for download."),
                receipt.memberId()));
    }
}
