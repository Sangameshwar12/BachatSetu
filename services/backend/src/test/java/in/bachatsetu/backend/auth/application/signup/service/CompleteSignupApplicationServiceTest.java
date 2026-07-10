package in.bachatsetu.backend.auth.application.signup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.signup.command.CompleteSignupCommand;
import in.bachatsetu.backend.auth.application.signup.exception.SignupApplicationException;
import in.bachatsetu.backend.auth.application.signup.exception.SignupFailureReason;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteSignupApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");
    private static final UserId USER_ID = UserId.newId();
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final OtpCode CODE = OtpCode.of("123456");

    private VerifyOtpUseCase verifyOtp;
    private UserRepository authUserRepository;
    private ProfileProvisioningPort profileProvisioning;
    private RoleRepository roleRepository;
    private GenerateAccessTokenUseCase generateAccessToken;
    private GenerateRefreshTokenUseCase generateRefreshToken;
    private TenantProvider tenantProvider;
    private DomainEventPublisherPort eventPublisher;
    private CompleteSignupApplicationService service;

    @BeforeEach
    void setUp() {
        verifyOtp = mock(VerifyOtpUseCase.class);
        authUserRepository = mock(UserRepository.class);
        profileProvisioning = mock(ProfileProvisioningPort.class);
        roleRepository = mock(RoleRepository.class);
        generateAccessToken = mock(GenerateAccessTokenUseCase.class);
        generateRefreshToken = mock(GenerateRefreshTokenUseCase.class);
        tenantProvider = mock(TenantProvider.class);
        eventPublisher = mock(DomainEventPublisherPort.class);

        when(tenantProvider.currentTenantId()).thenReturn(TENANT_ID);
        when(authUserRepository.findById(USER_ID)).thenReturn(Optional.of(pendingUser()));
        when(roleRepository.findByName("GROUP_MEMBER"))
                .thenReturn(Optional.of(Role.create(RoleId.newId(), "GROUP_MEMBER", TENANT_ID, NOW)));
        when(generateAccessToken.generate(any(GenerateAccessTokenCommand.class))).thenReturn(accessToken());
        when(generateRefreshToken.generate(any(GenerateRefreshTokenCommand.class))).thenReturn(refreshTokenResult());

        service = new CompleteSignupApplicationService(
                verifyOtp, authUserRepository, profileProvisioning, roleRepository, generateAccessToken,
                generateRefreshToken, tenantProvider, eventPublisher, () -> NOW);
    }

    @Test
    void activatesTheAccountAssignsTheDefaultRoleAndIssuesTokens() {
        when(verifyOtp.verify(any())).thenReturn(verifiedResult());

        SignupCompletedResult result = service.execute(new CompleteSignupCommand(USER_ID, CODE));

        assertThat(result.userId()).isEqualTo(USER_ID);
        verify(authUserRepository).save(any(User.class));
        verify(profileProvisioning).activateProfile(eq(USER_ID.toAggregateId()), any(), any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsCompletionWhenTheOtpHasExpired() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpExpired(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.REGISTRATION, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteSignupCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.OTP_EXPIRED));
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void rejectsCompletionWhenTheOtpCodeIsInvalid() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.REGISTRATION,
                OtpRejectionReason.INVALID_CODE, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteSignupCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.OTP_INVALID));
    }

    @Test
    void rejectsCompletionWhenVerificationAttemptsAreExhausted() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.REGISTRATION,
                OtpRejectionReason.ATTEMPT_LIMIT, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteSignupCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.OTP_ATTEMPTS_EXCEEDED));
    }

    private User pendingUser() {
        return User.register(
                USER_ID,
                new Email("asha@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)),
                TENANT_ID,
                NOW);
    }

    private OtpActionResult verifiedResult() {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(), USER_ID,
                in.bachatsetu.backend.auth.domain.model.OtpHash.encoded("A".repeat(64)), OtpPurpose.REGISTRATION,
                NOW, NOW.plusSeconds(300), OtpStatus.VERIFIED, 1, 0, AuditInfo.createdBy(TENANT_ID, NOW), 0);
        return OtpActionResult.from(verification, List.of());
    }

    private OtpActionResult eventResult(OtpApplicationEvent event) {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(), USER_ID,
                in.bachatsetu.backend.auth.domain.model.OtpHash.encoded("A".repeat(64)), OtpPurpose.REGISTRATION,
                NOW, NOW.plusSeconds(300), OtpStatus.FAILED, 1, 0, AuditInfo.createdBy(TENANT_ID, NOW), 0);
        return OtpActionResult.from(verification, List.of(event));
    }

    private IssuedAccessToken accessToken() {
        AccessTokenClaims claims = new AccessTokenClaims(
                USER_ID, MobileNumber.of("+919876543210"), TENANT_ID, Set.of("GROUP_MEMBER"), Set.of(), NOW,
                NOW.plusSeconds(900), "bachatsetu", "bachatsetu-api", 1);
        return new IssuedAccessToken(AccessTokenValue.of("token-value"), claims);
    }

    private RefreshTokenResult refreshTokenResult() {
        RefreshTokenCredential credential =
                RefreshTokenCredential.create(in.bachatsetu.backend.auth.domain.model.RefreshTokenId.newId(),
                        "s".repeat(32));
        return new RefreshTokenResult(credential, TokenSessionId.newId(), NOW.plusSeconds(2_592_000));
    }
}
