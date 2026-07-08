package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.exception.NotificationNotFoundException;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Notification aggregate. */
public final class GetNotificationApplicationService implements GetNotificationUseCase {

    private final NotificationRepository repository;
    private final TransactionPort transaction;
    private final NotificationApplicationMapper mapper;

    public GetNotificationApplicationService(
            NotificationRepository repository,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public NotificationResult execute(AggregateId tenantId, AggregateId notificationId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(notificationId, "notification id must not be null");
        return transaction.execute(() -> {
            Notification notification = repository.findById(tenantId, notificationId)
                    .orElseThrow(() -> new NotificationNotFoundException("notification does not exist"));
            return mapper.toResult(notification);
        });
    }
}
