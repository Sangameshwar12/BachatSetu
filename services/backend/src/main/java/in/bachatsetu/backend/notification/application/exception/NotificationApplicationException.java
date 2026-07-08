package in.bachatsetu.backend.notification.application.exception;

/** Base exception for application-level Notification failures. */
public class NotificationApplicationException extends RuntimeException {

    public NotificationApplicationException(String message) {
        super(message);
    }

    public NotificationApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
