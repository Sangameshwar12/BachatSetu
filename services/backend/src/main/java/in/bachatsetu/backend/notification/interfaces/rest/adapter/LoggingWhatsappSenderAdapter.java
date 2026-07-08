package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder WHATSAPP dispatch adapter: logs the outgoing message and reports a dummy successful send. No
 * WhatsApp Cloud API integration exists yet; this adapter exists solely to unblock the synchronous dispatch
 * flow until a real provider is integrated.
 */
public final class LoggingWhatsappSenderAdapter implements WhatsappSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWhatsappSenderAdapter.class);

    @Override
    public String send(NotificationRecipient recipient, NotificationContent content) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(content, "content must not be null");
        String providerMessageId = "WHATSAPP-" + UUID.randomUUID();
        LOGGER.info(
                "Simulated WHATSAPP send to {} -> providerMessageId={}",
                NotificationDestinationMasking.mask(recipient.destination()),
                providerMessageId);
        return providerMessageId;
    }
}
