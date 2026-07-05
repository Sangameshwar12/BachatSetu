package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RolePermissionTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void roleNormalizesNameAndRejectsDuplicatePermission() {
        AggregateId actorId = AggregateId.newId();
        RoleId roleId = RoleId.newId();
        Role role = Role.create(roleId, " group_organizer ", actorId, NOW);
        PermissionId permissionId = PermissionId.newId();

        role.grantPermission(permissionId, actorId, NOW.plusSeconds(1));

        assertThat(role.roleId()).isEqualTo(roleId);
        assertThat(role.name()).isEqualTo("GROUP_ORGANIZER");
        assertThat(role.permissionIds()).containsExactly(permissionId);
        assertThatThrownBy(() -> role.permissionIds().add(PermissionId.newId()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> role.grantPermission(permissionId, actorId, NOW.plusSeconds(2)))
                .isInstanceOf(IdentityDomainException.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Role.create(RoleId.newId(), "invalid role", actorId, NOW));
    }

    @Test
    void permissionNormalizesNameAndUsesIdentityEquality() {
        AggregateId actorId = AggregateId.newId();
        PermissionId permissionId = PermissionId.newId();
        Permission permission = Permission.create(permissionId, " Group:Read ", actorId, NOW);
        Permission sameIdentity = Permission.create(permissionId, "group:write", actorId, NOW);
        Permission different = Permission.create(PermissionId.newId(), "group:read", actorId, NOW);

        assertThat(permission.permissionId()).isEqualTo(permissionId);
        assertThat(permission.name()).isEqualTo("group:read");
        assertThat(permission).isEqualTo(permission).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(permission).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("permission");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Permission.create(PermissionId.newId(), "x", actorId, NOW));
    }

    @Test
    void roleEqualityUsesRoleIdentity() {
        AggregateId actorId = AggregateId.newId();
        RoleId roleId = RoleId.newId();
        Role first = Role.create(roleId, "MEMBER", actorId, NOW);
        Role sameIdentity = Role.create(roleId, "ORGANIZER", actorId, NOW);
        Role different = Role.create(RoleId.newId(), "MEMBER", actorId, NOW);

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("role");
    }

    @Test
    void rehydratesRolesAndPermissionsWithoutEvents() {
        AggregateId actorId = AggregateId.newId();
        var auditInfo = in.bachatsetu.backend.shared.domain.AuditInfo.createdBy(actorId, NOW);
        PermissionId permissionId = PermissionId.newId();
        Role role = Role.rehydrate(RoleId.newId(), "MEMBER", Set.of(permissionId), auditInfo, 4);
        Permission permission = Permission.rehydrate(
                permissionId, "group.read", auditInfo, 3);

        assertThat(role.permissionIds()).containsExactly(permissionId);
        assertThat(role.auditInfo()).isEqualTo(auditInfo);
        assertThat(role.version()).isEqualTo(4);
        assertThat(role.domainEvents()).isEmpty();
        assertThat(permission.auditInfo()).isEqualTo(auditInfo);
        assertThat(permission.version()).isEqualTo(3);
        assertThat(permission.domainEvents()).isEmpty();
    }
}
