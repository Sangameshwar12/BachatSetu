package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.application.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code LOGIN} audit entry when a {@code SIGN_IN} OTP is verified.
 *
 * <p>Reacts to the pre-existing {@link OtpVerified} application event rather than being called directly by
 * {@code VerifyOtpApplicationService}: the Auth module's REST layer already depends on Audit's REST layer
 * for nothing, and Audit's own REST layer necessarily depends on Auth (for {@code CurrentUserProvider}, like
 * every other module's controller) — a direct call the other way would create a module cycle. Listening to
 * an event Auth already publishes for its own reasons keeps the dependency one-directional.
 *
 * <p>{@code OtpVerified} fires for every purpose (sign-in, registration, ...), so this listener filters to
 * {@link OtpPurpose#SIGN_IN}. A login has no tenant yet at this point in the flow, so the recorded entry's
 * tenant is {@code null} — a tenant-less, identity-level event, exactly like the OTP challenge it reacts to.
 * A failure while recording is logged and swallowed rather than rethrown: login is the primary business
 * operation, and a best-effort audit entry must never appear to fail it.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LoginAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public LoginAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onOtpVerified(OtpVerified event) {
        if (event.purpose() != OtpPurpose.SIGN_IN) {
            return;
        }
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.LOGIN, "auth", "User",
                    event.userId().toAggregateId(), "LOGIN", "user signed in via OTP", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a login audit entry for user {}", event.userId(), exception);
        }
    }
}
