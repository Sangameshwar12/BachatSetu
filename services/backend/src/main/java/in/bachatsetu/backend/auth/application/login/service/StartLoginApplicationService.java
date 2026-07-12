package in.bachatsetu.backend.auth.application.login.service;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.login.command.StartLoginCommand;
import in.bachatsetu.backend.auth.application.login.exception.LoginApplicationException;
import in.bachatsetu.backend.auth.application.login.exception.LoginFailureReason;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;
import in.bachatsetu.backend.auth.application.login.usecase.StartLoginUseCase;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Starts a returning-user login: looks the account up by mobile number and dispatches a
 * {@code SIGN_IN} OTP through the already-wired OTP subsystem — the same
 * {@link GenerateOtpUseCase} signup uses, just with a different {@link OtpPurpose}. The account
 * itself is the actor for its own login attempt, matching {@code StartSignupApplicationService}'s
 * self-actor convention for pre-full-auth flows.
 */
public final class StartLoginApplicationService implements StartLoginUseCase {

    private final UserRepository userRepository;
    private final GenerateOtpUseCase generateOtp;

    public StartLoginApplicationService(UserRepository userRepository, GenerateOtpUseCase generateOtp) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.generateOtp = Objects.requireNonNull(generateOtp, "generateOtp must not be null");
    }

    @Override
    public LoginStartedResult execute(StartLoginCommand command) {
        Objects.requireNonNull(command, "start login command must not be null");
        User user = userRepository.findByMobileNumber(command.mobileNumber())
                .orElseThrow(() -> new LoginApplicationException(
                        LoginFailureReason.MOBILE_NOT_REGISTERED, "no account is registered for this mobile number"));
        if (user.status() != UserStatus.ACTIVE) {
            throw new LoginApplicationException(
                    LoginFailureReason.ACCOUNT_NOT_ACTIVE, "this account cannot sign in right now");
        }
        AggregateId actorId = user.userId().toAggregateId();
        OtpActionResult otpResult =
                generateOtp.generate(new GenerateOtpCommand(user.userId(), OtpPurpose.SIGN_IN, actorId));
        return new LoginStartedResult(user.userId(), command.mobileNumber().value(), otpResult.challenge().expiresAt());
    }
}
