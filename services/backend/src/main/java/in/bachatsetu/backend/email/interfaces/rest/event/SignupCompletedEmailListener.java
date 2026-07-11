package in.bachatsetu.backend.email.interfaces.rest.event;

import in.bachatsetu.backend.auth.domain.event.UserActivated;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
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
 * Sends the {@code SIGNUP_COMPLETED} email once a pending-verification account is activated
 * (after OTP verification). {@link UserActivated} carries only the user id, so this listener
 * reads the now-activated user's email through the existing {@link UserRepository} port —
 * exactly the same repository every OTP application service already reads through, not a new
 * capability.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SignupCompletedEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignupCompletedEmailListener.class);

    private final SendEmailUseCase sendEmail;
    private final UserRepository userRepository;

    public SignupCompletedEmailListener(SendEmailUseCase sendEmail, UserRepository userRepository) {
        this.sendEmail = Objects.requireNonNull(sendEmail, "sendEmail must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @EventListener
    public void onUserActivated(UserActivated event) {
        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null) {
                return;
            }
            sendEmail.execute(new SendEmailCommand(
                    new EmailAddress(user.email().value()), EmailTemplateCategory.SIGNUP_COMPLETED, Map.of()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to send a signup-completed email for user {}", event.userId(), exception);
        }
    }
}
