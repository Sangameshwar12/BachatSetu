package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Immutable permission aggregate with a canonical, case-insensitive name. */
public final class Permission extends BaseAggregateRoot {

    private final PermissionId permissionId;
    private final String name;

    private Permission(PermissionId permissionId, String name, AuditInfo auditInfo, long version) {
        super(permissionId.toAggregateId(), auditInfo, version);
        this.permissionId = Objects.requireNonNull(permissionId, "permission id must not be null");
        this.name = normalizeName(name);
    }

    public static Permission create(
            PermissionId permissionId,
            String name,
            AggregateId actorId,
            Instant createdAt) {
        return new Permission(permissionId, name, AuditInfo.createdBy(actorId, createdAt), 0);
    }

    /** Reconstructs persisted permission state without emitting domain events. */
    public static Permission rehydrate(
            PermissionId permissionId,
            String name,
            AuditInfo auditInfo,
            long version) {
        return new Permission(permissionId, name, auditInfo, version);
    }

    private static String normalizeName(String value) {
        Objects.requireNonNull(value, "permission name must not be null");
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3 || normalized.length() > 100
                || !normalized.matches("[a-z][a-z0-9]*(?:[.:_-][a-z0-9]+)*")) {
            throw new IllegalArgumentException("permission name format is invalid");
        }
        return normalized;
    }

    public PermissionId permissionId() {
        return permissionId;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Permission that && permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return permissionId.hashCode();
    }
}
