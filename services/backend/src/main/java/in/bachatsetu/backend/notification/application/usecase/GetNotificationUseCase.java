package in.bachatsetu.backend.notification.application.usecase;

import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped notification. */
@FunctionalInterface
public interface GetNotificationUseCase {

    NotificationResult execute(AggregateId tenantId, AggregateId notificationId);
}
