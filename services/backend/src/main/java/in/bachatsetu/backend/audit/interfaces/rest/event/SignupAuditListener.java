package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code USER_REGISTERED} audit entry when a self-registration creates a new
 * authentication user.
 *
 * <p>Reacts to the pre-existing {@link UserRegistered} domain event (now published through the
 * signup flow's {@code DomainEventPublisherPort}) rather than being called directly by the signup
 * application services, for the exact same reason documented on {@link LoginAuditListener}: Audit's
 * own REST layer already depends on Auth (for {@code CurrentUserProvider}), so a direct call the
 * other way would form a module cycle.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SignupAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignupAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public SignupAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onUserRegistered(UserRegistered event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.USER_REGISTERED, "auth", "User",
                    event.userId().toAggregateId(), "USER_REGISTERED", "self-registered a new account", null, null,
                    null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a signup audit entry for user {}", event.userId(), exception);
        }
    }
}
