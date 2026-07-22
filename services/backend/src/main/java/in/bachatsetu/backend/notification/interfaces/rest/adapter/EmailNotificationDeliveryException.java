package in.bachatsetu.backend.notification.interfaces.rest.adapter;

/** Raised by {@link RealEmailSenderAdapter} when the underlying {@code EmailSenderPort} reports a failed send. */
final class EmailNotificationDeliveryException extends RuntimeException {

    EmailNotificationDeliveryException(String message) {
        super(message);
    }
}
