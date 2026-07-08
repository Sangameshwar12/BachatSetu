package in.bachatsetu.backend.notification.application.exception;

/** Raised when a tenant-scoped notification lookup has no result. */
public final class NotificationNotFoundException extends NotificationApplicationException {

    public NotificationNotFoundException(String message) {
        super(message);
    }
}
