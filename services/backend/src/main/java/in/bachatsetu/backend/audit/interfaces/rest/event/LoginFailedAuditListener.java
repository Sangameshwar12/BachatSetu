package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code LOGIN_FAILED} audit entry when a {@code SIGN_IN} OTP is rejected or expires —
 * the failure-path counterpart to {@link LoginAuditListener}. Reacts to the pre-existing {@link
 * OtpRejected}/{@link OtpExpired} application events for the same one-directional-dependency
 * reason {@link LoginAuditListener} already documents: neither event carries a mobile number or
 * any other secret, only the OTP purpose and the account it was for.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LoginFailedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFailedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public LoginFailedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onOtpRejected(OtpRejected event) {
        if (event.purpose() != OtpPurpose.SIGN_IN) {
            return;
        }
        record(event.userId(), "invalid sign-in OTP: " + event.reason());
    }

    @EventListener
    public void onOtpExpired(OtpExpired event) {
        if (event.purpose() != OtpPurpose.SIGN_IN) {
            return;
        }
        record(event.userId(), "sign-in OTP expired");
    }

    private void record(UserId userId, String description) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, userId.toAggregateId(), AuditEventType.LOGIN_FAILED, "auth", "User",
                    userId.toAggregateId(), "LOGIN_FAILED", description, null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a login-failed audit entry for user {}", userId, exception);
        }
    }
}
