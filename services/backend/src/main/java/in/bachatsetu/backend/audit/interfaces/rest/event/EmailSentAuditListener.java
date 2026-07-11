package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.email.application.event.EmailSent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records an {@code EMAIL_SENT} audit entry whenever an email is successfully handed to a
 * provider. Reacts to {@link EmailSent} rather than being called directly by {@code
 * RetryingEmailSenderAdapter}, exactly as {@link OtpSentAuditListener} reacts to {@code OtpSent}:
 * it keeps Email's dependency on Audit one-directional. Metadata carries provider, duration, and
 * provider message id — never the recipient address in full or the email body.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EmailSentAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSentAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public EmailSentAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onEmailSent(EmailSent event) {
        try {
            String metadata = "{\"provider\":\"" + escape(event.provider())
                    + "\",\"providerMessageId\":\"" + escape(event.providerMessageId())
                    + "\",\"durationMillis\":" + event.durationMillis()
                    + ",\"recipientCount\":1,\"recipient\":\"" + escape(event.maskedRecipient()) + "\"}";
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, null, AuditEventType.EMAIL_SENT, "email", "Email", null, "EMAIL_SENT",
                    "email dispatched for " + event.category(), null, null, metadata));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an EMAIL_SENT audit entry for category {}", event.category(), exception);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
