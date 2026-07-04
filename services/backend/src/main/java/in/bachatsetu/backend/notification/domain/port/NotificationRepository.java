package in.bachatsetu.backend.notification.domain.port;

import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(AggregateId notificationId);

    void save(Notification notification);
}
