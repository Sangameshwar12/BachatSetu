package in.bachatsetu.backend.admin.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents one platform administrator acting on the platform. This is the domain seam for administrative
 * intent: it decides the target state of an action (for example enabling or disabling a user) without ever
 * touching persistence or another module's aggregate — the resulting {@link PlatformUserStatusChange} is
 * applied by the application layer through {@code PlatformUserRepository}.
 *
 * <p>Deliberately does not reach into {@code auth.domain.model.User}: enabling/disabling a user here is a
 * platform-wide, cross-tenant administrative action, whereas the Auth module's own {@code UserRepository} is
 * always scoped to the caller's own tenant. Keeping this decision in the Admin module's own domain avoids
 * redesigning the User aggregate or its repository.
 */
public final class PlatformAdministration {

    private final AggregateId administratorId;

    private PlatformAdministration(AggregateId administratorId) {
        this.administratorId = Objects.requireNonNull(administratorId, "administratorId must not be null");
    }

    public static PlatformAdministration actingAs(AggregateId administratorId) {
        return new PlatformAdministration(administratorId);
    }

    public AggregateId administratorId() {
        return administratorId;
    }

    /** Decides that {@code userId} should become {@link PlatformUserStatus#ACTIVE}. */
    public PlatformUserStatusChange enableUser(AggregateId userId, Instant at) {
        return decide(userId, PlatformUserStatus.ACTIVE, at);
    }

    /** Decides that {@code userId} should become {@link PlatformUserStatus#DISABLED}. */
    public PlatformUserStatusChange disableUser(AggregateId userId, Instant at) {
        return decide(userId, PlatformUserStatus.DISABLED, at);
    }

    private PlatformUserStatusChange decide(AggregateId userId, PlatformUserStatus targetStatus, Instant at) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(at, "at must not be null");
        return new PlatformUserStatusChange(userId, targetStatus, administratorId, at);
    }
}
