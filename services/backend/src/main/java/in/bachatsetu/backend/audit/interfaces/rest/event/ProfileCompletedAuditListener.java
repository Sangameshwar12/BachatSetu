package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.user.domain.event.ProfileCompleted;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code PROFILE_COMPLETED} audit entry when the post-signup onboarding step finishes.
 *
 * <p>Reacts to the pre-existing {@link ProfileCompleted} domain event rather than being called
 * directly by {@code CompleteOnboardingApplicationService}, for the same reason documented on
 * {@link LoginAuditListener}: Audit's own REST layer already depends on {@code auth} (for {@code
 * CurrentUserProvider}), so a direct call the other way would form a module cycle.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ProfileCompletedAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileCompletedAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public ProfileCompletedAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onProfileCompleted(ProfileCompleted event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.aggregateId(), AuditEventType.PROFILE_COMPLETED, "user", "UserProfile",
                    event.aggregateId(), "PROFILE_COMPLETED", "completed post-signup profile onboarding", null, null,
                    null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a profile-completed audit entry for {}", event.aggregateId(), exception);
        }
    }
}
