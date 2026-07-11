package in.bachatsetu.backend.email.interfaces.rest.event;

import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import in.bachatsetu.backend.email.application.command.SendEmailCommand;
import in.bachatsetu.backend.email.application.usecase.SendEmailUseCase;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Sends the {@code WELCOME} email when a new authentication user self-registers. Reacts to the
 * pre-existing {@link UserRegistered} domain event rather than being called directly by any
 * signup application service — the same one-directional-dependency reason documented on every
 * audit listener in this codebase (see {@code SignupAuditListener}) — so Auth has zero
 * compile-time dependency on Email. {@code UserRegistered} already carries the address, so no
 * repository lookup is needed. Email delivery is always best-effort: a provider outage must never
 * fail the signup flow that triggered it.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WelcomeEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeEmailListener.class);

    private final SendEmailUseCase sendEmail;

    public WelcomeEmailListener(SendEmailUseCase sendEmail) {
        this.sendEmail = Objects.requireNonNull(sendEmail, "sendEmail must not be null");
    }

    @EventListener
    public void onUserRegistered(UserRegistered event) {
        try {
            sendEmail.execute(new SendEmailCommand(
                    new EmailAddress(event.email().value()), EmailTemplateCategory.WELCOME, Map.of()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to send a welcome email for user {}", event.userId(), exception);
        }
    }
}
