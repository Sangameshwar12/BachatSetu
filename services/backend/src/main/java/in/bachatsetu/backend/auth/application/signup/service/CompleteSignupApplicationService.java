package in.bachatsetu.backend.auth.application.signup.service;

import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.signup.command.CompleteSignupCommand;
import in.bachatsetu.backend.auth.application.signup.exception.SignupApplicationException;
import in.bachatsetu.backend.auth.application.signup.exception.SignupFailureReason;
import in.bachatsetu.backend.auth.application.signup.query.SignupCompletedResult;
import in.bachatsetu.backend.auth.application.signup.usecase.CompleteSignupUseCase;
import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Verifies the signup OTP, activates both the profile and authentication aggregates, assigns the
 * default {@code GROUP_MEMBER} role, and issues the caller's first access and refresh tokens.
 *
 * <p>See {@link StartSignupApplicationService} for why profile activation goes through {@link
 * ProfileProvisioningPort} rather than the user module's own repository/domain types directly.
 */
public final class CompleteSignupApplicationService implements CompleteSignupUseCase {

    private static final String DEFAULT_ROLE_NAME = "GROUP_MEMBER";

    private final VerifyOtpUseCase verifyOtp;
    private final UserRepository authUserRepository;
    private final ProfileProvisioningPort profileProvisioning;
    private final RoleRepository roleRepository;
    private final GenerateAccessTokenUseCase generateAccessToken;
    private final GenerateRefreshTokenUseCase generateRefreshToken;
    private final TenantProvider tenantProvider;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;

    public CompleteSignupApplicationService(
            VerifyOtpUseCase verifyOtp,
            UserRepository authUserRepository,
            ProfileProvisioningPort profileProvisioning,
            RoleRepository roleRepository,
            GenerateAccessTokenUseCase generateAccessToken,
            GenerateRefreshTokenUseCase generateRefreshToken,
            TenantProvider tenantProvider,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock) {
        this.verifyOtp = Objects.requireNonNull(verifyOtp, "verifyOtp must not be null");
        this.authUserRepository = Objects.requireNonNull(authUserRepository, "authUserRepository must not be null");
        this.profileProvisioning = Objects.requireNonNull(profileProvisioning, "profileProvisioning must not be null");
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository must not be null");
        this.generateAccessToken = Objects.requireNonNull(generateAccessToken, "generateAccessToken must not be null");
        this.generateRefreshToken =
                Objects.requireNonNull(generateRefreshToken, "generateRefreshToken must not be null");
        this.tenantProvider = Objects.requireNonNull(tenantProvider, "tenantProvider must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SignupCompletedResult execute(CompleteSignupCommand command) {
        Objects.requireNonNull(command, "complete signup command must not be null");
        AggregateId actorId = command.userId().toAggregateId();
        OtpActionResult otpResult = verifyOtp.verify(
                new VerifyOtpCommand(command.userId(), OtpPurpose.REGISTRATION, command.code(), actorId));
        rejectIfNotVerified(otpResult);

        Instant now = clock.now();
        User user = authUserRepository.findById(command.userId())
                .orElseThrow(() -> new SignupApplicationException(
                        SignupFailureReason.OTP_INVALID, "no pending account exists for this user"));
        user.activate(actorId, now);
        RoleId groupMemberRoleId = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE_NAME + " role is not seeded"))
                .roleId();
        user.assignRole(groupMemberRoleId, actorId, now);
        authUserRepository.save(user);
        eventPublisher.publish(user.pullDomainEvents());

        profileProvisioning.activateProfile(actorId, actorId, now);

        AggregateId tenantId = tenantProvider.currentTenantId();
        IssuedAccessToken accessToken =
                generateAccessToken.generate(new GenerateAccessTokenCommand(command.userId(), tenantId));
        RefreshTokenResult refreshToken = generateRefreshToken.generate(new GenerateRefreshTokenCommand(
                command.userId(), tenantId, TokenSessionId.newId(), actorId));

        return new SignupCompletedResult(
                command.userId(), accessToken.token(), accessToken.expiresAt(), refreshToken.token(),
                refreshToken.expiresAt());
    }

    private void rejectIfNotVerified(OtpActionResult result) {
        boolean expired = result.events().stream().anyMatch(OtpExpired.class::isInstance);
        if (expired) {
            throw new SignupApplicationException(SignupFailureReason.OTP_EXPIRED, "the signup OTP has expired");
        }
        result.events().stream()
                .filter(OtpRejected.class::isInstance)
                .map(OtpRejected.class::cast)
                .findFirst()
                .ifPresent(rejected -> {
                    if (rejected.reason() == OtpRejectionReason.ATTEMPT_LIMIT) {
                        throw new SignupApplicationException(
                                SignupFailureReason.OTP_ATTEMPTS_EXCEEDED,
                                "the maximum OTP verification attempts have been exceeded");
                    }
                    throw new SignupApplicationException(SignupFailureReason.OTP_INVALID, "the supplied OTP is invalid");
                });
    }
}
