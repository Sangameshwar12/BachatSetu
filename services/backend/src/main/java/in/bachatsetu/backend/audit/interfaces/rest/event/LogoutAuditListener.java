package in.bachatsetu.backend.audit.interfaces.rest.event;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenRevoked;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records a {@code LOGOUT} audit entry when a refresh token is explicitly revoked.
 *
 * <p>Reacts to {@link RefreshTokenRevoked} — already registered by {@code RefreshToken.revoke()}
 * (Sprint 8.6) but never published until {@code RevokeRefreshTokenApplicationService} was wired
 * to a {@code DomainEventPublisherPort} this sprint, and only for the legitimate
 * active-token-revoked path: the reuse-detection security response inside
 * {@code RefreshAccessTokenApplicationService}/{@code RevokeRefreshTokenApplicationService} does
 * not publish, so a stolen-token replay never gets mislabeled as a user-initiated logout. Same
 * one-directional-dependency reasoning as {@link LoginAuditListener}.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LogoutAuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutAuditListener.class);

    private final CreateAuditEntryUseCase createAuditEntry;

    public LogoutAuditListener(CreateAuditEntryUseCase createAuditEntry) {
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @EventListener
    public void onRefreshTokenRevoked(RefreshTokenRevoked event) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    null, event.userId().toAggregateId(), AuditEventType.LOGOUT, "auth", "User",
                    event.userId().toAggregateId(), "LOGOUT", "refresh token revoked", null, null, null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to record a logout audit entry for user {}", event.userId(), exception);
        }
    }
}
