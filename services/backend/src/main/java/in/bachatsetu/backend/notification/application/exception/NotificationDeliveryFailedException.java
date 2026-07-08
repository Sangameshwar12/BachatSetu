package in.bachatsetu.backend.notification.application.exception;

/** Raised when a synchronous channel dispatch attempt fails during notification creation. */
public final class NotificationDeliveryFailedException extends NotificationApplicationException {

    public NotificationDeliveryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
