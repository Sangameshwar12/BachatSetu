package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder EMAIL dispatch adapter: logs the outgoing message and reports a dummy successful send. No SMTP
 * or provider integration exists yet; this adapter exists solely to unblock the synchronous dispatch flow
 * until a real provider is integrated.
 */
public final class LoggingEmailSenderAdapter implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public String send(NotificationRecipient recipient, NotificationContent content) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(content, "content must not be null");
        String providerMessageId = "EMAIL-" + UUID.randomUUID();
        LOGGER.info(
                "Simulated EMAIL send to {} (subject: {}) -> providerMessageId={}",
                NotificationDestinationMasking.mask(recipient.destination()),
                content.subject(),
                providerMessageId);
        return providerMessageId;
    }
}
