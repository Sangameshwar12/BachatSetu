package in.bachatsetu.backend.auth.application.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.login.command.CompleteLoginCommand;
import in.bachatsetu.backend.auth.application.login.exception.LoginApplicationException;
import in.bachatsetu.backend.auth.application.login.exception.LoginFailureReason;
import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
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
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
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

class CompleteLoginApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");
    private static final UserId USER_ID = UserId.newId();
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final OtpCode CODE = OtpCode.of("123456");

    private VerifyOtpUseCase verifyOtp;
    private UserRepository authUserRepository;
    private GenerateAccessTokenUseCase generateAccessToken;
    private GenerateRefreshTokenUseCase generateRefreshToken;
    private TenantProvider tenantProvider;
    private CompleteLoginApplicationService service;

    @BeforeEach
    void setUp() {
        verifyOtp = mock(VerifyOtpUseCase.class);
        authUserRepository = mock(UserRepository.class);
        generateAccessToken = mock(GenerateAccessTokenUseCase.class);
        generateRefreshToken = mock(GenerateRefreshTokenUseCase.class);
        tenantProvider = mock(TenantProvider.class);

        when(tenantProvider.currentTenantId()).thenReturn(TENANT_ID);
        when(authUserRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(generateAccessToken.generate(any(GenerateAccessTokenCommand.class))).thenReturn(accessToken());
        when(generateRefreshToken.generate(any(GenerateRefreshTokenCommand.class))).thenReturn(refreshTokenResult());

        service = new CompleteLoginApplicationService(
                verifyOtp, authUserRepository, generateAccessToken, generateRefreshToken, tenantProvider);
    }

    @Test
    void issuesTokensForAVerifiedReturningUser() {
        when(verifyOtp.verify(any())).thenReturn(verifiedResult());

        LoginCompletedResult result = service.execute(new CompleteLoginCommand(USER_ID, CODE));

        assertThat(result.userId()).isEqualTo(USER_ID);
        verify(generateAccessToken).generate(any(GenerateAccessTokenCommand.class));
        verify(generateRefreshToken).generate(any(GenerateRefreshTokenCommand.class));
    }

    @Test
    void rejectsCompletionWhenTheOtpHasExpired() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpExpired(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.SIGN_IN, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteLoginCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.OTP_EXPIRED));
        verify(generateAccessToken, never()).generate(any());
    }

    @Test
    void rejectsCompletionWhenTheOtpCodeIsInvalid() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.SIGN_IN,
                OtpRejectionReason.INVALID_CODE, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteLoginCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.OTP_INVALID));
    }

    @Test
    void rejectsCompletionWhenVerificationAttemptsAreExhausted() {
        when(verifyOtp.verify(any())).thenReturn(eventResult(new OtpRejected(
                UUID.randomUUID(), AggregateId.newId(), USER_ID, OtpPurpose.SIGN_IN,
                OtpRejectionReason.ATTEMPT_LIMIT, NOW)));

        assertThatThrownBy(() -> service.execute(new CompleteLoginCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.OTP_ATTEMPTS_EXCEEDED));
    }

    @Test
    void rejectsCompletionWhenTheAccountIsNoLongerActive() {
        when(verifyOtp.verify(any())).thenReturn(verifiedResult());
        when(authUserRepository.findById(USER_ID)).thenReturn(Optional.of(lockedUser()));

        assertThatThrownBy(() -> service.execute(new CompleteLoginCommand(USER_ID, CODE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.ACCOUNT_NOT_ACTIVE));
        verify(generateAccessToken, never()).generate(any());
    }

    private User activeUser() {
        return User.rehydrate(
                USER_ID, new Email("asha@example.com"), MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)), UserStatus.ACTIVE, Set.of(),
                AuditInfo.createdBy(TENANT_ID, NOW), 0);
    }

    private User lockedUser() {
        return User.rehydrate(
                USER_ID, new Email("asha@example.com"), MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)), UserStatus.LOCKED, Set.of(),
                AuditInfo.createdBy(TENANT_ID, NOW), 0);
    }

    private OtpActionResult verifiedResult() {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(), USER_ID, OtpHash.encoded("A".repeat(64)), OtpPurpose.SIGN_IN,
                NOW, NOW.plusSeconds(300), OtpStatus.VERIFIED, 1, 0, AuditInfo.createdBy(TENANT_ID, NOW), 0);
        return OtpActionResult.from(verification, List.of());
    }

    private OtpActionResult eventResult(OtpApplicationEvent event) {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(), USER_ID, OtpHash.encoded("A".repeat(64)), OtpPurpose.SIGN_IN,
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
        RefreshTokenCredential credential = RefreshTokenCredential.create(RefreshTokenId.newId(), "s".repeat(32));
        return new RefreshTokenResult(credential, TokenSessionId.newId(), NOW.plusSeconds(2_592_000));
    }
}
