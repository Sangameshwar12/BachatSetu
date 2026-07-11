package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.email.application.event.EmailSendFailed;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records an {@code EMAIL_SEND_FAILED} audit entry when the email delivery infrastructure
 * exhausts every retry attempt (see {@code RetryingEmailSenderAdapter}). Mirrors {@link
 * OtpSendFailedAuditListener} exactly.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EmailSendFailedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendFailedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public EmailSendFailedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onEmailSendFailed(EmailSendFailed event) {
        try {
            String metadata = "{\"provider\":\"" + escape(event.provider())
                    + "\",\"failureReason\":\"" + escape(event.failureReason())
                    + "\",\"durationMillis\":" + event.durationMillis()
                    + ",\"recipientCount\":1,\"recipient\":\"" + escape(event.maskedRecipient()) + "\"}";
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, null, AuditEventType.EMAIL_SEND_FAILED, "email", "Email", null, "EMAIL_SEND_FAILED",
                    "email delivery via " + event.provider() + " failed for " + event.category(), null, null,
                    metadata));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an EMAIL_SEND_FAILED audit entry for category {}", event.category(), exception);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
