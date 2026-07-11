package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpSendFailed;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records an {@code OTP_SEND_FAILED} audit entry when the SMS delivery infrastructure exhausts
 * every retry attempt (see {@code SmsOtpSenderAdapter}). Reacts to {@link OtpSendFailed} rather
 * than being called directly by that adapter for the same dependency-direction reason as every
 * other listener in this package. The recorded metadata (provider name, failure reason) is
 * infrastructure diagnostic information already scrubbed of secrets by the adapter that
 * published the event — never the OTP code or the phone number, neither of which this event
 * carries in the first place.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OtpSendFailedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpSendFailedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public OtpSendFailedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onOtpSendFailed(OtpSendFailed event) {
        try {
            String metadata = "{\"provider\":\"" + escape(event.provider())
                    + "\",\"failureReason\":\"" + escape(event.failureReason()) + "\"}";
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.OTP_SEND_FAILED, "auth", "User",
                    event.userId().toAggregateId(), "OTP_SEND_FAILED",
                    "OTP delivery via " + event.provider() + " failed for " + event.purpose(),
                    null, null, metadata));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an OTP_SEND_FAILED audit entry for user {}", event.userId(), exception);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
