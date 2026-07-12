package in.bachatsetu.backend.security.context;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the actor recorded in every entity's {@code created_by}/{@code updated_by} columns.
 *
 * <p>For authenticated requests, returns the real signed-in user's id — read from the same
 * {@link AuthenticatedUser} principal that {@link in.bachatsetu.backend.security.filter.JwtAuthenticationFilter}
 * already places in the {@link SecurityContextHolder} for every JWT-authenticated call (see
 * {@link CurrentUserService}, which reads the identical principal for application use cases).
 * Pre-authentication flows (signup, OTP request) have no such principal yet; those fall back to
 * {@link #systemActorId}, a configured placeholder distinct from any real user id.
 *
 * <p>This is generic Spring Data JPA bookkeeping metadata, not the system's primary audit trail
 * — see the {@code audit} module's {@code audit.audit_entries} table for the business-event audit
 * log, which already threads the real acting user's id through explicit application-layer
 * parameters independent of this class.
 */
public final class SecurityContextCurrentAuditorProvider implements CurrentAuditorProvider {

    private final UUID systemActorId;

    public SecurityContextCurrentAuditorProvider(UUID systemActorId) {
        this.systemActorId = Objects.requireNonNull(systemActorId, "systemActorId must not be null");
    }

    @Override
    public Optional<UUID> currentAuditorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId().value());
        }
        return Optional.of(systemActorId);
    }
}
