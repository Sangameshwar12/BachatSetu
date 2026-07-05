package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Role aggregate containing permission identifiers. */
public final class Role extends BaseAggregateRoot {

    private final RoleId roleId;
    private final String name;
    private final Set<PermissionId> permissionIds;

    private Role(
            RoleId roleId,
            String name,
            Set<PermissionId> permissionIds,
            AuditInfo auditInfo,
            long version) {
        super(roleId.toAggregateId(), auditInfo, version);
        this.roleId = Objects.requireNonNull(roleId, "role id must not be null");
        this.name = normalizeName(name);
        this.permissionIds = new LinkedHashSet<>(
                Objects.requireNonNull(permissionIds, "permission ids must not be null"));
    }

    public static Role create(RoleId roleId, String name, AggregateId actorId, Instant createdAt) {
        return new Role(roleId, name, Set.of(), AuditInfo.createdBy(actorId, createdAt), 0);
    }

    /** Reconstructs persisted role state without emitting domain events. */
    public static Role rehydrate(
            RoleId roleId,
            String name,
            Set<PermissionId> permissionIds,
            AuditInfo auditInfo,
            long version) {
        return new Role(roleId, name, permissionIds, auditInfo, version);
    }

    public void grantPermission(PermissionId permissionId, AggregateId actorId, Instant grantedAt) {
        Objects.requireNonNull(permissionId, "permission id must not be null");
        if (!permissionIds.add(permissionId)) {
            throw new IdentityDomainException("role already has the permission");
        }
        markChanged(actorId, grantedAt);
    }

    private static String normalizeName(String value) {
        Objects.requireNonNull(value, "role name must not be null");
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() < 2 || normalized.length() > 64 || !normalized.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("role name format is invalid");
        }
        return normalized;
    }

    public RoleId roleId() {
        return roleId;
    }

    public String name() {
        return name;
    }

    public Set<PermissionId> permissionIds() {
        return Set.copyOf(permissionIds);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Role that && roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return roleId.hashCode();
    }
}
