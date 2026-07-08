package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.command.MarkNotificationFailedCommand;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import java.util.Objects;

/** Coordinates the SENDING/SENT-to-FAILED notification transition without owning business invariants. */
public final class MarkNotificationFailedApplicationService implements MarkNotificationFailedUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final NotificationApplicationSupport support;

    public MarkNotificationFailedApplicationService(
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
    public NotificationResult execute(MarkNotificationFailedCommand command) {
        Objects.requireNonNull(command, "mark failed command must not be null");
        return transaction.execute(() -> {
            Notification notification = support.requireNotification(command.tenantId(), command.notificationId());
            notification.markFailed(command.failureCode(), command.actorId(), clock.now());
            return support.saveAndPublish(notification);
        });
    }
}
