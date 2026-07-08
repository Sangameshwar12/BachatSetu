package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.command.MarkNotificationDeliveredCommand;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import java.util.Objects;

/** Coordinates the QUEUED-to-DELIVERED notification transition without owning business invariants. */
public final class MarkNotificationDeliveredApplicationService implements MarkNotificationDeliveredUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final NotificationApplicationSupport support;

    public MarkNotificationDeliveredApplicationService(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new NotificationApplicationSupport(
                Objects.requireNonNull(repository, "repository must not be null"), eventPublisher, mapper);
    }

    @Override
    public NotificationResult execute(MarkNotificationDeliveredCommand command) {
        Objects.requireNonNull(command, "mark delivered command must not be null");
        return transaction.execute(() -> {
            Notification notification = support.requireNotification(command.tenantId(), command.notificationId());
            notification.markDelivered(command.actorId(), clock.now());
            return support.saveAndPublish(notification);
        });
    }
}
