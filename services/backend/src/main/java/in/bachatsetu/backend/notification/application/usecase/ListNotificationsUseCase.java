package in.bachatsetu.backend.notification.application.usecase;

import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact notification views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListNotificationsUseCase {

    NotificationPage<NotificationSummary> execute(AggregateId tenantId, NotificationPageRequest pageRequest);
}
