package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Collection;
import java.util.Objects;

/** Creates permissions while enforcing name uniqueness against an authoritative set. */
public final class PermissionFactory {

    private final Clock clock;

    public PermissionFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Permission createUnique(
            String name,
            Collection<Permission> existingPermissions,
            AggregateId actorId) {
        Objects.requireNonNull(existingPermissions, "existing permissions must not be null");
        Permission candidate = Permission.create(
                PermissionId.newId(), name, actorId, clock.instant());
        boolean duplicate = existingPermissions.stream()
                .filter(Objects::nonNull)
                .anyMatch(permission -> permission.name().equals(candidate.name()));
        if (duplicate) {
            throw new IdentityDomainException("permission name must be unique");
        }
        return candidate;
    }
}
