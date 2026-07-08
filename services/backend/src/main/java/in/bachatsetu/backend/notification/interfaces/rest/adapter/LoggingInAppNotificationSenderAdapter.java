package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder in-app dispatch adapter: logs the outgoing message and reports a dummy successful send. No
 * push/in-app delivery infrastructure exists yet; this adapter exists solely to unblock the synchronous
 * dispatch flow until a real provider is integrated.
 */
public final class LoggingInAppNotificationSenderAdapter implements InAppNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInAppNotificationSenderAdapter.class);

    @Override
    public String send(NotificationRecipient recipient, NotificationContent content) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(content, "content must not be null");
        String providerMessageId = "IN_APP-" + UUID.randomUUID();
        LOGGER.info(
                "Simulated in-app send to user {} -> providerMessageId={}",
                recipient.userId().value(),
                providerMessageId);
        return providerMessageId;
    }
}
