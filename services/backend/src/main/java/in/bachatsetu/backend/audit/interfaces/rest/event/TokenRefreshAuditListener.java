package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenCreated;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code TOKEN_REFRESH} audit entry when a refresh token is rotated.
 *
 * <p>Reacts to {@link RefreshTokenCreated} — already registered by {@code RefreshToken.issue()}
 * (Sprint 8.6). {@code RefreshAccessTokenApplicationService} only publishes it for the
 * replacement token it creates during rotation, not the initial login/signup issuance (that
 * already produces its own {@code LOGIN} entry), so this listener never double-counts a sign-in
 * as a refresh. Same one-directional-dependency reasoning as {@link LoginAuditListener}.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TokenRefreshAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public TokenRefreshAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onRefreshTokenCreated(RefreshTokenCreated event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.TOKEN_REFRESH, "auth", "User",
                    event.userId().toAggregateId(), "TOKEN_REFRESH", "access token refreshed", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a token-refresh audit entry for user {}", event.userId(), exception);
        }
    }
}
