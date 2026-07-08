package in.bachatsetu.backend.notification.application.mapper;

import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.domain.model.DeliveryAttempt;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Maps the Notification domain aggregate to immutable application query models. */
public final class NotificationApplicationMapper {

    public NotificationResult toResult(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        return new NotificationResult(
                notification.id().value(),
                notification.tenantId().value(),
                notification.recipient().userId().value(),
                notification.recipient().destination(),
                notification.channel().name(),
                notification.category().name(),
                notification.content().subject(),
                notification.content().body(),
                notification.status().name(),
                notification.scheduledAt(),
                notification.auditInfo().createdAt(),
                notification.auditInfo().updatedAt(),
                deliveredAt(notification),
                failureReason(notification),
                notification.version());
    }

    public NotificationSummary toSummary(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        return new NotificationSummary(
                notification.id().value(),
                notification.channel().name(),
                notification.category().name(),
                notification.status().name(),
                notification.scheduledAt(),
                notification.auditInfo().createdAt());
    }

    private Instant deliveredAt(Notification notification) {
        return notification.status() == NotificationStatus.DELIVERED
                ? notification.auditInfo().updatedAt()
                : null;
    }

    private String failureReason(Notification notification) {
        if (notification.status() != NotificationStatus.FAILED) {
            return null;
        }
        List<DeliveryAttempt> attempts = notification.attempts();
        return attempts.isEmpty() ? null : attempts.getLast().failureCode();
    }
}
