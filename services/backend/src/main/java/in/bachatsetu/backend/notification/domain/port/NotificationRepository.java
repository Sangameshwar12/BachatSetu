package in.bachatsetu.backend.notification.domain.port;

import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(AggregateId notificationId);

    Optional<Notification> findById(AggregateId tenantId, AggregateId notificationId);

    NotificationPage<Notification> findPage(AggregateId tenantId, NotificationPageRequest pageRequest);

    /** Whether a notification of {@code category} was already created for {@code recipientUserId} at or
     * after {@code since}. Used by the automation reminder jobs (Sprint 12.1) to avoid queuing more than
     * one reminder of the same kind for the same recipient on the same day. */
    boolean existsForRecipientSince(
            AggregateId tenantId, AggregateId recipientUserId, NotificationCategory category, Instant since);

    /** The 5 most recently created notifications for a given recipient, newest first. */
    List<Notification> findRecentForRecipient(AggregateId tenantId, AggregateId recipientUserId);

    void save(Notification notification);
}
