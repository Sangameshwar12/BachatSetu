package in.bachatsetu.backend.notification.domain.port;

import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(AggregateId notificationId);

    Optional<Notification> findById(AggregateId tenantId, AggregateId notificationId);

    NotificationPage<Notification> findPage(AggregateId tenantId, NotificationPageRequest pageRequest);

    void save(Notification notification);
}
