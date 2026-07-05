package in.bachatsetu.backend.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class IdentityJpaMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final String PASSWORD_HASH = "$2b$12$" + "A".repeat(53);

    @Test
    void mapsAuthenticationUserAndRoleAssociations() {
        AuthUserJpaMapper mapper = Mappers.getMapper(AuthUserJpaMapper.class);
        UserJpaEntity entity = mock(UserJpaEntity.class);
        RoleJpaEntity role = mock(RoleJpaEntity.class);
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        stubAudit(entity, userId, 6);
        when(entity.getEmail()).thenReturn("member@example.com");
        when(entity.getPhoneNumber()).thenReturn("+919876543210");
        when(entity.getPasswordHash()).thenReturn(PASSWORD_HASH);
        when(entity.getAuthenticationStatus()).thenReturn(UserStatus.ACTIVE);
        when(entity.getRoles()).thenReturn(Set.of(role));
        when(role.getId()).thenReturn(roleId);

        User domain = mapper.toDomain(entity);

        assertThat(domain.userId()).isEqualTo(new UserId(userId));
        assertThat(domain.roleIds()).containsExactly(new RoleId(roleId));
        assertThat(domain.version()).isEqualTo(6);
        assertThat(domain.domainEvents()).isEmpty();
    }

    @Test
    void mapsRoleAndPermissionCanonicalNames() {
        RoleJpaMapper roleMapper = Mappers.getMapper(RoleJpaMapper.class);
        PermissionJpaMapper permissionMapper = Mappers.getMapper(PermissionJpaMapper.class);
        PermissionJpaEntity permissionEntity = mock(PermissionJpaEntity.class);
        RoleJpaEntity roleEntity = mock(RoleJpaEntity.class);
        UUID permissionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        stubAudit(permissionEntity, permissionId, 2);
        stubAudit(roleEntity, roleId, 3);
        when(permissionEntity.getCode()).thenReturn("GROUP.READ");
        when(roleEntity.getCode()).thenReturn("GROUP_MEMBER");
        when(roleEntity.getPermissions()).thenReturn(Set.of(permissionEntity));

        Permission permission = permissionMapper.toDomain(permissionEntity);
        Role role = roleMapper.toDomain(roleEntity);

        assertThat(permission.name()).isEqualTo("group.read");
        assertThat(role.name()).isEqualTo("GROUP_MEMBER");
        assertThat(role.permissionIds()).containsExactly(new PermissionId(permissionId));
    }

    @Test
    void mapsRefreshTokenAndOtpInBothDirections() {
        RefreshTokenJpaMapper tokenMapper = Mappers.getMapper(RefreshTokenJpaMapper.class);
        OtpVerificationJpaMapper otpMapper = Mappers.getMapper(OtpVerificationJpaMapper.class);
        JpaReferenceProvider references = mock(JpaReferenceProvider.class);
        UserJpaEntity userReference = mock(UserJpaEntity.class);
        UserId userId = UserId.newId();
        AggregateId actorId = AggregateId.newId();
        when(references.user(userId.toAggregateId())).thenReturn(userReference);
        RefreshToken token = RefreshToken.rehydrate(
                RefreshTokenId.newId(),
                userId,
                AggregateId.newId(),
                TokenSessionId.newId(),
                RefreshTokenHash.encoded("H".repeat(60)),
                NOW,
                NOW.plusSeconds(3600),
                TokenStatus.REVOKED,
                null,
                AuditInfo.createdBy(actorId, NOW),
                4);
        OtpVerification otp = OtpVerification.rehydrate(
                AggregateId.newId(),
                userId,
                OtpHash.encoded("C".repeat(64)),
                OtpPurpose.SIGN_IN,
                NOW,
                NOW.plusSeconds(300),
                OtpStatus.PENDING,
                2,
                1,
                AuditInfo.createdBy(actorId, NOW),
                1);

        RefreshTokenJpaEntity tokenEntity = tokenMapper.toEntity(token, references);
        OtpVerificationJpaEntity otpEntity = otpMapper.toEntity(otp, references);

        assertThat(tokenEntity.getUser()).isSameAs(userReference);
        assertThat(tokenEntity.getStatus()).isEqualTo(TokenStatus.REVOKED);
        assertThat(tokenEntity.getTokenHash()).isEqualTo("H".repeat(60));
        assertThat(otpEntity.getUser()).isSameAs(userReference);
        assertThat(otpEntity.getHash()).isEqualTo("C".repeat(64));
        assertThat(otpEntity.getPurpose()).isEqualTo(OtpPurpose.SIGN_IN);
        assertThat(otpEntity.getVerificationAttempts()).isEqualTo(2);
        assertThat(otpEntity.getResendCount()).isOne();
    }

    @Test
    void updatesCanonicalAuthenticationEntityWithoutProfileMutation() {
        AuthUserJpaMapper mapper = Mappers.getMapper(AuthUserJpaMapper.class);
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Asha",
                "Patil",
                "member@example.com",
                "+919876543210",
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE,
                in.bachatsetu.backend.user.domain.model.PreferredLanguage.MARATHI);
        RoleJpaEntity role = mock(RoleJpaEntity.class);
        User domain = User.rehydrate(
                new UserId(entity.getId()),
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded(PASSWORD_HASH),
                UserStatus.ACTIVE,
                Set.of(new RoleId(UUID.randomUUID())),
                AuditInfo.createdBy(AggregateId.newId(), NOW),
                0);

        mapper.updateEntity(domain, entity, Set.of(role));

        assertThat(entity.getGivenName()).isEqualTo("Asha");
        assertThat(entity.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(entity.getAuthenticationStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(entity.getRoles()).containsExactly(role);
    }

    private void stubAudit(BaseJpaEntity entity, UUID id, long version) {
        UUID actorId = UUID.randomUUID();
        when(entity.getId()).thenReturn(id);
        when(entity.getCreatedAt()).thenReturn(NOW);
        when(entity.getUpdatedAt()).thenReturn(NOW.plusSeconds(1));
        when(entity.getCreatedBy()).thenReturn(actorId);
        when(entity.getUpdatedBy()).thenReturn(actorId);
        when(entity.getVersion()).thenReturn(version);
    }
}
