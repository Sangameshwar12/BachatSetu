package in.bachatsetu.backend.notification.domain.model;

public enum NotificationStatus {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
}
