package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import java.util.Objects;

/**
 * Bridges the notification module's already-rendered content to the shared {@link EmailSenderPort}
 * — the same port {@code email}'s own {@code SendEmailApplicationService} uses — so a real
 * provider (SES/Resend/SendGrid) or its local logging fallback handles delivery exactly as it
 * does for every other email BachatSetu sends. Whether that turns out to be real or log-only is
 * decided entirely by which {@link EmailSenderPort} bean is active; this adapter never knows or
 * cares, and needs no configuration of its own.
 */
public final class RealEmailSenderAdapter implements EmailSender {

    private static final String DEFAULT_SUBJECT = "BachatSetu notification";

    private final EmailSenderPort emailSenderPort;

    public RealEmailSenderAdapter(EmailSenderPort emailSenderPort) {
        this.emailSenderPort = Objects.requireNonNull(emailSenderPort, "emailSenderPort must not be null");
    }

    @Override
    public String send(NotificationRecipient recipient, NotificationContent content) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(content, "content must not be null");
        EmailMessage message = new EmailMessage(
                new EmailAddress(recipient.destination()), EmailTemplateCategory.GENERAL_NOTIFICATION,
                toEmailContent(content));
        EmailSendResult result = emailSenderPort.send(message);
        if (result.status() == EmailDeliveryStatus.FAILED) {
            throw new EmailNotificationDeliveryException(result.failureReason());
        }
        return result.providerMessageId();
    }

    private EmailContent toEmailContent(NotificationContent content) {
        String subject = content.subject() != null ? content.subject() : DEFAULT_SUBJECT;
        return new EmailContent(subject, toHtml(content.body()), content.body());
    }

    /** Escapes the plain-text notification body for safe HTML display, preserving line breaks. */
    private String toHtml(String body) {
        String escaped = body
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<p>" + escaped + "</p>";
    }
}
