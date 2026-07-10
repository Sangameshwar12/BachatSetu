package in.bachatsetu.backend.auth.application.signup.service;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.signup.command.StartSignupCommand;
import in.bachatsetu.backend.auth.application.signup.exception.SignupApplicationException;
import in.bachatsetu.backend.auth.application.signup.exception.SignupFailureReason;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.application.signup.usecase.StartSignupUseCase;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Creates a self-registered account across both the {@code user} (profile) and {@code auth}
 * (authentication) bounded contexts and dispatches the registration OTP. Both aggregates persist
 * onto the same {@code identity.users} row: the profile is provisioned first (its repository can
 * insert a fresh row), then the authentication side is populated onto that same, now-existing row
 * — this composes two already-existing repository behaviors rather than changing either one.
 *
 * <p>Profile provisioning goes through {@link ProfileProvisioningPort} rather than the user
 * module's own repository/domain types directly: the user module's onboarding REST layer depends
 * on auth (for {@code CurrentUserProvider}), so a direct auth-side dependency on user's application
 * or domain layer would form a package cycle. The port's adapter lives in general infrastructure,
 * which may depend on any module's domain layer.
 *
 * <p>Publishes the {@code auth} {@code User}'s domain events (notably {@code UserRegistered})
 * through {@link DomainEventPublisherPort} rather than calling the Audit module directly: Audit's
 * own REST layer already depends on {@code auth.application.security.CurrentUserProvider}, like
 * every other module's controller, so a direct call the other way would form a package cycle. A
 * dedicated Audit-side listener reacts to the published event instead — the same pattern already
 * used for {@code LOGIN} auditing.
 */
public final class StartSignupApplicationService implements StartSignupUseCase {

    private static final Pattern MOBILE_DIGITS = Pattern.compile("[^0-9]");

    private final ProfileProvisioningPort profileProvisioning;
    private final UserRepository authUserRepository;
    private final GenerateOtpUseCase generateOtp;
    private final PasswordHashGeneratorPort passwordHashGenerator;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;

    public StartSignupApplicationService(
            ProfileProvisioningPort profileProvisioning,
            UserRepository authUserRepository,
            GenerateOtpUseCase generateOtp,
            PasswordHashGeneratorPort passwordHashGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock) {
        this.profileProvisioning = Objects.requireNonNull(profileProvisioning, "profileProvisioning must not be null");
        this.authUserRepository = Objects.requireNonNull(authUserRepository, "authUserRepository must not be null");
        this.generateOtp = Objects.requireNonNull(generateOtp, "generateOtp must not be null");
        this.passwordHashGenerator =
                Objects.requireNonNull(passwordHashGenerator, "passwordHashGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SignupStartedResult execute(StartSignupCommand command) {
        Objects.requireNonNull(command, "start signup command must not be null");
        if (!command.acceptedTerms()) {
            throw new SignupApplicationException(
                    SignupFailureReason.TERMS_NOT_ACCEPTED, "terms and conditions must be accepted to sign up");
        }
        PhoneNumber phoneNumber = new PhoneNumber(command.mobileNumber().value());
        if (profileProvisioning.existsByPhoneNumber(phoneNumber)) {
            throw new SignupApplicationException(
                    SignupFailureReason.MOBILE_ALREADY_REGISTERED, "mobile number is already registered");
        }
        if (command.email() != null && profileProvisioning.existsByEmail(command.email())) {
            throw new SignupApplicationException(
                    SignupFailureReason.EMAIL_ALREADY_REGISTERED, "email is already registered");
        }

        AggregateId newId = AggregateId.newId();
        UserId userId = new UserId(newId.value());
        Instant now = clock.now();

        profileProvisioning.createProfile(
                newId, command.givenName(), command.familyName(), command.email(), phoneNumber,
                command.preferredLanguage(), newId, now);

        Email accountEmail =
                command.email() != null ? command.email() : placeholderEmail(command.mobileNumber().value());
        PasswordHash passwordHash = passwordHashGenerator.generateRandom();
        User user = User.register(userId, accountEmail, command.mobileNumber(), passwordHash, newId, now);
        authUserRepository.save(user);
        eventPublisher.publish(user.pullDomainEvents());

        OtpActionResult otpResult =
                generateOtp.generate(new GenerateOtpCommand(userId, OtpPurpose.REGISTRATION, newId));
        return new SignupStartedResult(userId, command.mobileNumber().value(), otpResult.challenge().expiresAt());
    }

    private Email placeholderEmail(String mobileNumberValue) {
        String digits = MOBILE_DIGITS.matcher(mobileNumberValue).replaceAll("");
        return new Email(digits + "@no-email.bachatsetu.invalid");
    }
}
