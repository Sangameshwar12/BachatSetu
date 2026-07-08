package in.bachatsetu.backend.notification.application.port;

import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;

/**
 * Outbound port for dispatching an in-app notification.
 *
 * <p>Backed by {@link in.bachatsetu.backend.notification.domain.model.NotificationChannel#PUSH}: the
 * pre-existing domain enum and the {@code notification.notifications} schema (both shipped ahead of this
 * sprint) already model this channel as {@code PUSH} rather than {@code IN_APP}. Renaming the enum value
 * would require a Flyway migration to widen {@code ck_notifications_channel}, which this additive sprint
 * avoids; the port is named to match the sprint's vocabulary while the channel it serves keeps its existing
 * name.
 */
@FunctionalInterface
public interface InAppNotificationSender {

    /** Dispatches the message and returns a provider-assigned message identifier. */
    String send(NotificationRecipient recipient, NotificationContent content);
}
