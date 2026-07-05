package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenPrincipal;
import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Builds an authorization snapshot from authoritative domain repositories. */
public final class TokenPrincipalResolver {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public TokenPrincipalResolver(
            UserRepository users,
            RoleRepository roles,
            PermissionRepository permissions) {
        this.users = Objects.requireNonNull(users, "user repository must not be null");
        this.roles = Objects.requireNonNull(roles, "role repository must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permission repository must not be null");
    }

    public AccessTokenPrincipal resolve(UserId userId, AggregateId tenantId) {
        User user = users.findById(Objects.requireNonNull(userId, "user id must not be null"))
                .orElseThrow(() -> failure(TokenFailureReason.USER_NOT_FOUND, "authentication user does not exist"));
        if (user.status() != UserStatus.ACTIVE) {
            throw failure(TokenFailureReason.USER_NOT_ACTIVE, "authentication user is not active");
        }
        Set<String> roleNames = new TreeSet<>();
        Set<String> permissionNames = new TreeSet<>();
        user.roleIds().forEach(roleId -> {
            Role role = roles.findById(roleId)
                    .orElseThrow(() -> failure(TokenFailureReason.ROLE_NOT_FOUND, "assigned role does not exist"));
            roleNames.add(role.name());
            role.permissionIds().forEach(permissionId -> {
                Permission permission = permissions.findById(permissionId)
                        .orElseThrow(() -> failure(
                                TokenFailureReason.PERMISSION_NOT_FOUND,
                                "assigned permission does not exist"));
                permissionNames.add(permission.name());
            });
        });
        return new AccessTokenPrincipal(user.userId(), user.mobileNumber(), tenantId, roleNames, permissionNames);
    }

    private TokenApplicationException failure(TokenFailureReason reason, String message) {
        return new TokenApplicationException(reason, message);
    }
}
