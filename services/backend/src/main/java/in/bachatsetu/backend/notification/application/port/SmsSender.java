package in.bachatsetu.backend.notification.application.port;

import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;

/** Outbound port for dispatching a notification over the SMS channel. */
@FunctionalInterface
public interface SmsSender {

    /** Dispatches the message and returns a provider-assigned message identifier. */
    String send(NotificationRecipient recipient, NotificationContent content);
}
