package in.bachatsetu.backend.notification.interfaces.rest.event;

import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.payment.domain.event.PaymentStatusChanged;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies a payment's member and the owning group's organizer once a payment is verified.
 *
 * <p>Reacts to the pre-existing {@link PaymentStatusChanged} domain event rather than being called directly
 * by any Payment application service, so the Payment module holds no compile-time dependency on Notification
 * (the ports-and-events integration this sprint requires). {@link PaymentStatusChanged} fires for every
 * status transition, so this listener filters to {@link PaymentStatus#VERIFIED} and ignores every other
 * transition.
 *
 * <p>Listens with {@link TransactionPhase#AFTER_COMMIT} so a notification is only attempted once the
 * verifying transaction has durably committed. A failure while notifying is logged and swallowed rather than
 * rethrown: verification is the primary business operation, and a best-effort notification must never appear
 * to fail it.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentVerifiedNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentVerifiedNotificationListener.class);

    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final CreateNotificationUseCase createNotification;

    public PaymentVerifiedNotificationListener(
            PaymentRepository paymentRepository,
            GroupRepository groupRepository,
            CreateNotificationUseCase createNotification) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "payment repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentStatusChanged(PaymentStatusChanged event) {
        if (event.currentStatus() != PaymentStatus.VERIFIED) {
            return;
        }
        try {
            notifyPaymentVerified(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a payment-verified notification for payment {}", event.aggregateId(), exception);
        }
    }

    private void notifyPaymentVerified(PaymentStatusChanged event) {
        Payment payment = paymentRepository.findById(event.aggregateId()).orElse(null);
        if (payment == null) {
            return;
        }
        notify(payment.tenantId(), payment.memberId(), "Payment Received",
                "Your contribution has been successfully verified.");

        groupRepository.findById(payment.groupId()).ifPresent(group -> notifyOrganizer(payment, group));
    }

    private void notifyOrganizer(Payment payment, SavingsGroup group) {
        if (group.organizerId().equals(payment.memberId())) {
            return;
        }
        notify(payment.tenantId(), group.organizerId(), "Payment Received",
                "A member's contribution has been verified in " + group.name().value() + ".");
    }

    private void notify(AggregateId tenantId, AggregateId recipientUserId, String title, String body) {
        createNotification.execute(new CreateNotificationCommand(
                tenantId,
                recipientUserId,
                recipientUserId.value().toString(),
                NotificationChannel.PUSH,
                NotificationCategory.PAYMENT,
                Map.of("title", title, "body", body),
                recipientUserId));
    }
}
