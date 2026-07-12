package in.bachatsetu.backend.auth.application.login.service;

import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.login.command.CompleteLoginCommand;
import in.bachatsetu.backend.auth.application.login.exception.LoginApplicationException;
import in.bachatsetu.backend.auth.application.login.exception.LoginFailureReason;
import in.bachatsetu.backend.auth.application.login.query.LoginCompletedResult;
import in.bachatsetu.backend.auth.application.login.usecase.CompleteLoginUseCase;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Verifies the sign-in OTP started by {@link StartLoginApplicationService} and issues the
 * caller's access and refresh tokens through the already-wired token subsystem — no new token
 * generation logic, only a different {@link OtpPurpose} and no account-activation/role-assignment
 * side effects, since a returning user is already {@link UserStatus#ACTIVE}.
 */
public final class CompleteLoginApplicationService implements CompleteLoginUseCase {

    private final VerifyOtpUseCase verifyOtp;
    private final UserRepository authUserRepository;
    private final GenerateAccessTokenUseCase generateAccessToken;
    private final GenerateRefreshTokenUseCase generateRefreshToken;
    private final TenantProvider tenantProvider;

    public CompleteLoginApplicationService(
            VerifyOtpUseCase verifyOtp,
            UserRepository authUserRepository,
            GenerateAccessTokenUseCase generateAccessToken,
            GenerateRefreshTokenUseCase generateRefreshToken,
            TenantProvider tenantProvider) {
        this.verifyOtp = Objects.requireNonNull(verifyOtp, "verifyOtp must not be null");
        this.authUserRepository = Objects.requireNonNull(authUserRepository, "authUserRepository must not be null");
        this.generateAccessToken = Objects.requireNonNull(generateAccessToken, "generateAccessToken must not be null");
        this.generateRefreshToken =
                Objects.requireNonNull(generateRefreshToken, "generateRefreshToken must not be null");
        this.tenantProvider = Objects.requireNonNull(tenantProvider, "tenantProvider must not be null");
    }

    @Override
    public LoginCompletedResult execute(CompleteLoginCommand command) {
        Objects.requireNonNull(command, "complete login command must not be null");
        AggregateId actorId = command.userId().toAggregateId();
        OtpActionResult otpResult = verifyOtp.verify(
                new VerifyOtpCommand(command.userId(), OtpPurpose.SIGN_IN, command.code(), actorId));
        rejectIfNotVerified(otpResult);

        User user = authUserRepository.findById(command.userId())
                .orElseThrow(() -> new LoginApplicationException(
                        LoginFailureReason.OTP_INVALID, "no account exists for this user"));
        if (user.status() != UserStatus.ACTIVE) {
            throw new LoginApplicationException(
                    LoginFailureReason.ACCOUNT_NOT_ACTIVE, "this account cannot sign in right now");
        }

        AggregateId tenantId = tenantProvider.currentTenantId();
        IssuedAccessToken accessToken =
                generateAccessToken.generate(new GenerateAccessTokenCommand(command.userId(), tenantId));
        RefreshTokenResult refreshToken = generateRefreshToken.generate(new GenerateRefreshTokenCommand(
                command.userId(), tenantId, TokenSessionId.newId(), actorId));

        return new LoginCompletedResult(
                command.userId(), accessToken.token(), accessToken.expiresAt(), refreshToken.token(),
                refreshToken.expiresAt());
    }

    private void rejectIfNotVerified(OtpActionResult result) {
        boolean expired = result.events().stream().anyMatch(OtpExpired.class::isInstance);
        if (expired) {
            throw new LoginApplicationException(LoginFailureReason.OTP_EXPIRED, "the sign-in OTP has expired");
        }
        result.events().stream()
                .filter(OtpRejected.class::isInstance)
                .map(OtpRejected.class::cast)
                .findFirst()
                .ifPresent(rejected -> {
                    if (rejected.reason() == OtpRejectionReason.ATTEMPT_LIMIT) {
                        throw new LoginApplicationException(
                                LoginFailureReason.OTP_ATTEMPTS_EXCEEDED,
                                "the maximum OTP verification attempts have been exceeded");
                    }
                    throw new LoginApplicationException(LoginFailureReason.OTP_INVALID, "the supplied OTP is invalid");
                });
    }
}
