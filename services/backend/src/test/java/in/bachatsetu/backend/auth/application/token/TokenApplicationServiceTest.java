package in.bachatsetu.backend.auth.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.RefreshAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.RevokeRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.ValidateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenPrincipal;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.IssuedRefreshToken;
import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.application.token.service.GenerateAccessTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.GenerateRefreshTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.RefreshAccessTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.RefreshTokenCredentialVerifier;
import in.bachatsetu.backend.auth.application.token.service.RevokeRefreshTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.TokenPrincipalResolver;
import in.bachatsetu.backend.auth.application.token.service.ValidateAccessTokenApplicationService;
import in.bachatsetu.backend.auth.domain.exception.RefreshTokenConflictException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final Duration REFRESH_LIFETIME = Duration.ofDays(30);
    private static final UserId USER_ID = UserId.newId();
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId ACTOR_ID = AggregateId.newId();
    private static final TokenSessionId SESSION_ID = TokenSessionId.newId();
    private static final RefreshTokenHash HASH = RefreshTokenHash.encoded("H".repeat(60));

    @Mock private UserRepository users;
    @Mock private RoleRepository roles;
    @Mock private PermissionRepository permissions;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private JwtProviderPort jwtProvider;
    @Mock private TokenHasherPort hasher;
    @Mock private TokenClockPort clock;
    @Mock private DomainEventPublisherPort eventPublisher;

    private TokenPrincipalResolver principals;
    private RefreshTokenCredentialVerifier verifier;
    private IssuedAccessToken issuedAccess;

    @BeforeEach
    void setUp() {
        principals = new TokenPrincipalResolver(users, roles, permissions);
        verifier = new RefreshTokenCredentialVerifier(refreshTokens, hasher);
        AccessTokenClaims claims = new AccessTokenClaims(
                USER_ID,
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"),
                NOW,
                NOW.plusSeconds(900),
                "bachatsetu",
                "bachatsetu-api",
                1);
        issuedAccess = new IssuedAccessToken(AccessTokenValue.of("header.payload.signature"), claims);
    }

    @Test
    void resolvesPrincipalAndGeneratesAccessToken() {
        stubActivePrincipal();
        when(jwtProvider.issue(any())).thenReturn(issuedAccess);
        var service = new GenerateAccessTokenApplicationService(principals, jwtProvider);

        IssuedAccessToken result = service.generate(new GenerateAccessTokenCommand(USER_ID, TENANT_ID));

        assertThat(result).isEqualTo(issuedAccess);
        ArgumentCaptor<AccessTokenPrincipal> principal = ArgumentCaptor.forClass(AccessTokenPrincipal.class);
        verify(jwtProvider).issue(principal.capture());
        assertThat(principal.getValue().roles()).containsExactly("GROUP_MEMBER");
        assertThat(principal.getValue().permissions()).containsExactly("group.read");
    }

    @Test
    void rejectsMissingInactiveAndStalePrincipalAssociations() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());
        assertReason(() -> principals.resolve(USER_ID, TENANT_ID), TokenFailureReason.USER_NOT_FOUND);

        when(users.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.LOCKED, Set.of())));
        assertReason(() -> principals.resolve(USER_ID, TENANT_ID), TokenFailureReason.USER_NOT_ACTIVE);

        RoleId roleId = RoleId.newId();
        when(users.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE, Set.of(roleId))));
        when(roles.findById(roleId)).thenReturn(Optional.empty());
        assertReason(() -> principals.resolve(USER_ID, TENANT_ID), TokenFailureReason.ROLE_NOT_FOUND);

        PermissionId permissionId = PermissionId.newId();
        when(roles.findById(roleId)).thenReturn(Optional.of(role(roleId, permissionId)));
        when(permissions.findById(permissionId)).thenReturn(Optional.empty());
        assertReason(() -> principals.resolve(USER_ID, TENANT_ID), TokenFailureReason.PERMISSION_NOT_FOUND);
    }

    @Test
    void generatesHashedRefreshTokenAndExpiresStaleSessionToken() {
        stubActivePrincipal();
        when(clock.now()).thenReturn(NOW);
        RefreshToken stale = token(TokenStatus.ACTIVE, NOW, null);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(stale));
        stubIssuedRefresh();
        var service = generateRefreshService();

        var result = service.generate(new GenerateRefreshTokenCommand(
                USER_ID, TENANT_ID, SESSION_ID, ACTOR_ID));

        assertThat(stale.status()).isEqualTo(TokenStatus.EXPIRED);
        assertThat(result.token().toString()).isEqualTo("[REDACTED]");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(REFRESH_LIFETIME));
        verify(refreshTokens).save(stale);
        verify(refreshTokens, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rejectsSecondActiveTokenAndInvalidLifetime() {
        stubActivePrincipal();
        when(clock.now()).thenReturn(NOW);
        when(refreshTokens.findActive(USER_ID, SESSION_ID))
                .thenReturn(Optional.of(token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null)));
        var service = generateRefreshService();

        assertReason(
                () -> service.generate(new GenerateRefreshTokenCommand(
                        USER_ID, TENANT_ID, SESSION_ID, ACTOR_ID)),
                TokenFailureReason.ACTIVE_REFRESH_TOKEN_EXISTS);
        assertThatIllegalArgumentException().isThrownBy(() -> new GenerateRefreshTokenApplicationService(
                principals, refreshTokens, hasher, clock, Duration.ZERO));
    }

    @Test
    void verifiesOpaqueRefreshCredentialsWithoutLookupDisclosure() {
        RefreshToken token = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        RefreshTokenCredential credential = credential(token.refreshTokenId());
        when(refreshTokens.findById(token.refreshTokenId())).thenReturn(Optional.of(token));
        when(hasher.matches(any(RefreshTokenCredential.class), any(RefreshTokenHash.class))).thenReturn(true);

        assertThat(verifier.verify(credential.value())).isEqualTo(token);

        assertReason(() -> verifier.verify("malformed"), TokenFailureReason.INVALID_REFRESH_TOKEN);
        when(refreshTokens.findById(any())).thenReturn(Optional.empty());
        assertReason(
                () -> verifier.verify(credential(RefreshTokenId.newId()).value()),
                TokenFailureReason.INVALID_REFRESH_TOKEN);
        when(refreshTokens.findById(token.refreshTokenId())).thenReturn(Optional.of(token));
        when(hasher.matches(any(), any())).thenReturn(false);
        assertReason(() -> verifier.verify(credential.value()), TokenFailureReason.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rotatesRefreshTokenAndReturnsNewPair() {
        stubActivePrincipal();
        RefreshToken current = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(current);
        stubIssuedRefresh();
        when(clock.now()).thenReturn(NOW);
        when(jwtProvider.issue(any())).thenReturn(issuedAccess);
        var service = refreshService();

        TokenPairResult result = service.refresh(new RefreshAccessTokenCommand(
                credential(current.refreshTokenId()).value(), ACTOR_ID));

        assertThat(result.accessToken()).isEqualTo(issuedAccess);
        assertThat(current.status()).isEqualTo(TokenStatus.ROTATED);
        assertThat(current.replacedByTokenId()).isNotNull();
        verify(refreshTokens).replace(any(), any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsExpiredRevokedAndAlreadyReusedRefreshTokens() {
        when(clock.now()).thenReturn(NOW);
        assertRefreshFailure(token(TokenStatus.ACTIVE, NOW, null), TokenFailureReason.REFRESH_TOKEN_EXPIRED);
        assertRefreshFailure(token(TokenStatus.EXPIRED, NOW.plusSeconds(60), null), TokenFailureReason.REFRESH_TOKEN_EXPIRED);
        assertRefreshFailure(token(TokenStatus.REVOKED, NOW.plusSeconds(60), null), TokenFailureReason.REFRESH_TOKEN_REVOKED);
        assertRefreshFailure(
                token(TokenStatus.REUSED, NOW.plusSeconds(60), RefreshTokenId.newId()),
                TokenFailureReason.REFRESH_TOKEN_REUSED);
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshAccessTokenApplicationService(
                verifier,
                refreshTokens,
                principals,
                jwtProvider,
                hasher,
                clock,
                Duration.ZERO,
                eventPublisher));
    }

    @Test
    void translatesAConcurrentRotationConflictIntoARefreshTokenConflictReason() {
        stubActivePrincipal();
        RefreshToken current = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(current);
        stubIssuedRefresh();
        when(clock.now()).thenReturn(NOW);
        when(jwtProvider.issue(any())).thenReturn(issuedAccess);
        org.mockito.Mockito.doThrow(new RefreshTokenConflictException("conflict", new RuntimeException("cause")))
                .when(refreshTokens).replace(any(), any());

        assertReason(
                () -> refreshService().refresh(new RefreshAccessTokenCommand(
                        credential(current.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_CONFLICT);
    }

    @Test
    void translatesAConcurrentReuseRecordingConflictIntoARefreshTokenConflictReason() {
        RefreshToken rotated = token(TokenStatus.ROTATED, NOW.plusSeconds(60), RefreshTokenId.newId());
        RefreshToken active = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(rotated);
        when(clock.now()).thenReturn(NOW);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(active));
        org.mockito.Mockito.doThrow(new RefreshTokenConflictException("conflict", new RuntimeException("cause")))
                .when(refreshTokens).recordReuse(any(), any());

        assertReason(
                () -> refreshService().refresh(new RefreshAccessTokenCommand(
                        credential(rotated.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_CONFLICT);
    }

    @Test
    void detectsRotationReuseAndRevokesActiveReplacement() {
        RefreshToken rotated = token(TokenStatus.ROTATED, NOW.plusSeconds(60), RefreshTokenId.newId());
        RefreshToken active = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(rotated);
        when(clock.now()).thenReturn(NOW);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(active));

        assertReason(
                () -> refreshService().refresh(new RefreshAccessTokenCommand(
                        credential(rotated.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_REUSED);

        assertThat(rotated.status()).isEqualTo(TokenStatus.REUSED);
        assertThat(active.status()).isEqualTo(TokenStatus.REVOKED);
        verify(refreshTokens).recordReuse(rotated, Optional.of(active));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void expiresActiveReplacementWhenReuseIsDetectedAfterItsDeadline() {
        RefreshToken rotated = token(TokenStatus.ROTATED, NOW.plusSeconds(60), RefreshTokenId.newId());
        RefreshToken active = token(TokenStatus.ACTIVE, NOW, null);
        stubVerified(rotated);
        when(clock.now()).thenReturn(NOW);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(active));

        assertReason(
                () -> refreshService().refresh(new RefreshAccessTokenCommand(
                        credential(rotated.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_REUSED);

        assertThat(active.status()).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void revokesActiveTokenIdempotentlyAndRejectsInvalidStates() {
        when(clock.now()).thenReturn(NOW);
        RefreshToken active = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(active);
        var service = new RevokeRefreshTokenApplicationService(verifier, refreshTokens, clock, eventPublisher);

        assertThat(service.revoke(new RevokeRefreshTokenCommand(
                        credential(active.refreshTokenId()).value(), ACTOR_ID)).status())
                .isEqualTo(TokenStatus.REVOKED);
        assertThat(service.revoke(new RevokeRefreshTokenCommand(
                        credential(active.refreshTokenId()).value(), ACTOR_ID)).status())
                .isEqualTo(TokenStatus.REVOKED);
        verify(eventPublisher, times(1)).publish(any());

        assertRevokeFailure(token(TokenStatus.ACTIVE, NOW, null), TokenFailureReason.REFRESH_TOKEN_EXPIRED);
        assertRevokeFailure(token(TokenStatus.EXPIRED, NOW.plusSeconds(1), null), TokenFailureReason.REFRESH_TOKEN_EXPIRED);
        assertRevokeFailure(
                token(TokenStatus.ROTATED, NOW.plusSeconds(1), RefreshTokenId.newId()),
                TokenFailureReason.REFRESH_TOKEN_REUSED);
        assertRevokeFailure(
                token(TokenStatus.REUSED, NOW.plusSeconds(1), RefreshTokenId.newId()),
                TokenFailureReason.REFRESH_TOKEN_REUSED);
    }

    @Test
    void detectsReuseDuringRevocationAndDisablesReplacement() {
        when(clock.now()).thenReturn(NOW);
        RefreshToken rotated = token(TokenStatus.ROTATED, NOW.plusSeconds(60), RefreshTokenId.newId());
        RefreshToken active = token(TokenStatus.ACTIVE, NOW.plusSeconds(60), null);
        stubVerified(rotated);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(active));

        assertReason(
                () -> new RevokeRefreshTokenApplicationService(verifier, refreshTokens, clock, eventPublisher)
                        .revoke(new RevokeRefreshTokenCommand(
                                credential(rotated.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_REUSED);

        assertThat(rotated.status()).isEqualTo(TokenStatus.REUSED);
        assertThat(active.status()).isEqualTo(TokenStatus.REVOKED);
        verify(refreshTokens).recordReuse(rotated, Optional.of(active));

        RefreshToken secondRotated = token(TokenStatus.ROTATED, NOW.plusSeconds(60), RefreshTokenId.newId());
        RefreshToken expiredReplacement = token(TokenStatus.ACTIVE, NOW, null);
        stubVerified(secondRotated);
        when(refreshTokens.findActive(USER_ID, SESSION_ID)).thenReturn(Optional.of(expiredReplacement));
        assertReason(
                () -> new RevokeRefreshTokenApplicationService(verifier, refreshTokens, clock, eventPublisher)
                        .revoke(new RevokeRefreshTokenCommand(
                                credential(secondRotated.refreshTokenId()).value(), ACTOR_ID)),
                TokenFailureReason.REFRESH_TOKEN_REUSED);
        assertThat(expiredReplacement.status()).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void validatesAccessTokenThroughProvider() {
        when(jwtProvider.validate(issuedAccess.token())).thenReturn(issuedAccess.claims());
        var service = new ValidateAccessTokenApplicationService(jwtProvider);

        assertThat(service.validate(new ValidateAccessTokenCommand(issuedAccess.token())))
                .isEqualTo(issuedAccess.claims());
    }

    private GenerateRefreshTokenApplicationService generateRefreshService() {
        return new GenerateRefreshTokenApplicationService(
                principals, refreshTokens, hasher, clock, REFRESH_LIFETIME);
    }

    private RefreshAccessTokenApplicationService refreshService() {
        return new RefreshAccessTokenApplicationService(
                verifier,
                refreshTokens,
                principals,
                jwtProvider,
                hasher,
                clock,
                REFRESH_LIFETIME,
                eventPublisher);
    }

    private void assertRefreshFailure(RefreshToken token, TokenFailureReason reason) {
        stubVerified(token);
        assertReason(
                () -> refreshService().refresh(new RefreshAccessTokenCommand(
                        credential(token.refreshTokenId()).value(), ACTOR_ID)),
                reason);
    }

    private void assertRevokeFailure(RefreshToken token, TokenFailureReason reason) {
        stubVerified(token);
        assertReason(
                () -> new RevokeRefreshTokenApplicationService(verifier, refreshTokens, clock, eventPublisher)
                        .revoke(new RevokeRefreshTokenCommand(
                                credential(token.refreshTokenId()).value(), ACTOR_ID)),
                reason);
    }

    private void assertReason(Runnable action, TokenFailureReason reason) {
        assertThatThrownBy(action::run)
                .isInstanceOf(TokenApplicationException.class)
                .extracting(exception -> ((TokenApplicationException) exception).reason())
                .isEqualTo(reason);
    }

    private void stubActivePrincipal() {
        RoleId roleId = RoleId.newId();
        PermissionId permissionId = PermissionId.newId();
        when(users.findById(USER_ID)).thenReturn(Optional.of(user(UserStatus.ACTIVE, Set.of(roleId))));
        when(roles.findById(roleId)).thenReturn(Optional.of(role(roleId, permissionId)));
        when(permissions.findById(permissionId)).thenReturn(Optional.of(Permission.rehydrate(
                permissionId,
                "group.read",
                AuditInfo.createdBy(ACTOR_ID, NOW),
                0)));
    }

    private void stubIssuedRefresh() {
        when(hasher.issue(any())).thenAnswer(invocation -> {
            RefreshTokenId id = invocation.getArgument(0);
            return new IssuedRefreshToken(credential(id), HASH);
        });
    }

    private void stubVerified(RefreshToken token) {
        when(refreshTokens.findById(token.refreshTokenId())).thenReturn(Optional.of(token));
        when(hasher.matches(any(RefreshTokenCredential.class), any(RefreshTokenHash.class))).thenReturn(true);
    }

    private User user(UserStatus status, Set<RoleId> roleIds) {
        return User.rehydrate(
                USER_ID,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)),
                status,
                roleIds,
                AuditInfo.createdBy(ACTOR_ID, NOW),
                0);
    }

    private Role role(RoleId roleId, PermissionId permissionId) {
        return Role.rehydrate(
                roleId,
                "GROUP_MEMBER",
                Set.of(permissionId),
                AuditInfo.createdBy(ACTOR_ID, NOW),
                0);
    }

    private RefreshToken token(TokenStatus status, Instant expiry, RefreshTokenId replacementId) {
        return RefreshToken.rehydrate(
                RefreshTokenId.newId(),
                USER_ID,
                TENANT_ID,
                SESSION_ID,
                HASH,
                NOW.minusSeconds(60),
                expiry,
                status,
                replacementId,
                AuditInfo.createdBy(ACTOR_ID, NOW.minusSeconds(60)),
                0);
    }

    private RefreshTokenCredential credential(RefreshTokenId tokenId) {
        return RefreshTokenCredential.create(tokenId, "S".repeat(43));
    }
}
