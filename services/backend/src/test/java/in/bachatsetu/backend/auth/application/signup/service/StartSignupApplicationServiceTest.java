package in.bachatsetu.backend.auth.application.signup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.signup.command.StartSignupCommand;
import in.bachatsetu.backend.auth.application.signup.exception.SignupApplicationException;
import in.bachatsetu.backend.auth.application.signup.exception.SignupFailureReason;
import in.bachatsetu.backend.auth.application.signup.query.SignupStartedResult;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartSignupApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");
    private static final MobileNumber MOBILE = MobileNumber.of("+919876543210");

    private ProfileProvisioningPort profileProvisioning;
    private UserRepository authUserRepository;
    private GenerateOtpUseCase generateOtp;
    private PasswordHashGeneratorPort passwordHashGenerator;
    private DomainEventPublisherPort eventPublisher;
    private StartSignupApplicationService service;

    @BeforeEach
    void setUp() {
        profileProvisioning = mock(ProfileProvisioningPort.class);
        authUserRepository = mock(UserRepository.class);
        generateOtp = mock(GenerateOtpUseCase.class);
        passwordHashGenerator = mock(PasswordHashGeneratorPort.class);
        eventPublisher = mock(DomainEventPublisherPort.class);

        when(profileProvisioning.existsByPhoneNumber(any())).thenReturn(false);
        when(profileProvisioning.existsByEmail(any())).thenReturn(false);
        when(passwordHashGenerator.generateRandom())
                .thenReturn(PasswordHash.encoded("$2b$12$" + "A".repeat(53)));
        when(generateOtp.generate(any())).thenReturn(otpResult());

        service = new StartSignupApplicationService(
                profileProvisioning, authUserRepository, generateOtp, passwordHashGenerator, eventPublisher,
                () -> NOW);
    }

    @Test
    void createsProfileThenAuthUserAndDispatchesARegistrationOtp() {
        StartSignupCommand command = new StartSignupCommand(
                "Asha", "Rao", MOBILE, new Email("asha@example.com"), "ENGLISH", true);

        SignupStartedResult result = service.execute(command);

        verify(profileProvisioning).createProfile(
                any(), eq("Asha"), eq("Rao"), any(), any(), eq("ENGLISH"), any(), any());
        verify(authUserRepository).save(any(User.class));
        verify(eventPublisher).publish(any());
        verify(generateOtp).generate(any(GenerateOtpCommand.class));
        assertThat(result.mobileNumber()).isEqualTo(MOBILE.value());
    }

    @Test
    void synthesizesAPlaceholderEmailWhenNoneIsSupplied() {
        StartSignupCommand command =
                new StartSignupCommand("Asha", null, MOBILE, null, "ENGLISH", true);

        service.execute(command);

        verify(profileProvisioning, never()).existsByEmail(any());
        verify(authUserRepository).save(any(User.class));
    }

    @Test
    void rejectsSignupWhenTermsAreNotAccepted() {
        StartSignupCommand command = new StartSignupCommand(
                "Asha", "Rao", MOBILE, new Email("asha@example.com"), "ENGLISH", false);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.TERMS_NOT_ACCEPTED));
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void rejectsSignupWhenTheMobileNumberIsAlreadyRegistered() {
        when(profileProvisioning.existsByPhoneNumber(new PhoneNumber(MOBILE.value()))).thenReturn(true);
        StartSignupCommand command = new StartSignupCommand(
                "Asha", "Rao", MOBILE, new Email("asha@example.com"), "ENGLISH", true);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.MOBILE_ALREADY_REGISTERED));
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void rejectsSignupWhenTheEmailIsAlreadyRegistered() {
        Email email = new Email("asha@example.com");
        when(profileProvisioning.existsByEmail(email)).thenReturn(true);
        StartSignupCommand command =
                new StartSignupCommand("Asha", "Rao", MOBILE, email, "ENGLISH", true);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOfSatisfying(SignupApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(SignupFailureReason.EMAIL_ALREADY_REGISTERED));
        verify(authUserRepository, never()).save(any());
    }

    private OtpActionResult otpResult() {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(),
                new in.bachatsetu.backend.auth.domain.model.UserId(AggregateId.newId().value()),
                in.bachatsetu.backend.auth.domain.model.OtpHash.encoded("A".repeat(64)),
                OtpPurpose.REGISTRATION,
                NOW,
                NOW.plusSeconds(300),
                OtpStatus.PENDING,
                0,
                0,
                AuditInfo.createdBy(AggregateId.newId(), NOW),
                0);
        return OtpActionResult.from(verification, List.of());
    }
}
