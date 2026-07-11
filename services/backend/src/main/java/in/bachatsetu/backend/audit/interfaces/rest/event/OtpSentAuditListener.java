package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpSent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records an {@code OTP_SENT} audit entry whenever an OTP is successfully handed to the SMS
 * delivery infrastructure — reacting to {@link OtpSent}, exactly as {@link LoginAuditListener}
 * reacts to {@code OtpVerified}, for the same reason: it keeps Auth's dependency on Audit
 * one-directional. Sprint PI-2.1 is the first time {@code GenerateOtpApplicationService}/
 * {@code ResendOtpApplicationService} actually publish this event (previously built but never
 * wired to a publisher — see those services' {@code eventPublisher} field); this listener is
 * what finally puts the {@code OTP_SENT} audit event type to use. No tenant exists yet at OTP
 * generation, matching {@link LoginAuditListener}'s tenant-less {@code LOGIN} entry.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OtpSentAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpSentAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public OtpSentAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onOtpSent(OtpSent event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.OTP_SENT, "auth", "User",
                    event.userId().toAggregateId(), "OTP_SENT",
                    "OTP dispatched for " + event.purpose(), null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record an OTP_SENT audit entry for user {}", event.userId(), exception);
        }
    }
}
