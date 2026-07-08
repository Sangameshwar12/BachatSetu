package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.exception.NotificationNotFoundException;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class NotificationApplicationSupport {

    private final NotificationRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final NotificationApplicationMapper mapper;

    NotificationApplicationSupport(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            NotificationApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    Notification requireNotification(AggregateId tenantId, AggregateId notificationId) {
        return repository.findById(tenantId, notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("notification does not exist"));
    }

    NotificationResult saveAndPublish(Notification notification) {
        repository.save(notification);
        eventPublisher.publish(notification.pullDomainEvents());
        return mapper.toResult(notification);
    }
}
