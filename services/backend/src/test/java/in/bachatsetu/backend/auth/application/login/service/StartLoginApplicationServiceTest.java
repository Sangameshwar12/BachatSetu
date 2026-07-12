package in.bachatsetu.backend.auth.application.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.login.command.StartLoginCommand;
import in.bachatsetu.backend.auth.application.login.exception.LoginApplicationException;
import in.bachatsetu.backend.auth.application.login.exception.LoginFailureReason;
import in.bachatsetu.backend.auth.application.login.query.LoginStartedResult;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartLoginApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");
    private static final MobileNumber MOBILE = MobileNumber.of("+919876543210");
    private static final UserId USER_ID = UserId.newId();

    private UserRepository userRepository;
    private GenerateOtpUseCase generateOtp;
    private StartLoginApplicationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        generateOtp = mock(GenerateOtpUseCase.class);
        service = new StartLoginApplicationService(userRepository, generateOtp);
    }

    @Test
    void dispatchesASignInOtpForAnActiveAccount() {
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(activeUser()));
        when(generateOtp.generate(any())).thenReturn(otpResult());

        LoginStartedResult result = service.execute(new StartLoginCommand(MOBILE));

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.mobileNumber()).isEqualTo(MOBILE.value());
        verify(generateOtp).generate(any(GenerateOtpCommand.class));
    }

    @Test
    void rejectsLoginWhenNoAccountIsRegisteredForTheMobileNumber() {
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new StartLoginCommand(MOBILE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.MOBILE_NOT_REGISTERED));
        verify(generateOtp, never()).generate(any());
    }

    @Test
    void rejectsLoginWhenTheAccountIsNotActive() {
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(pendingUser()));

        assertThatThrownBy(() -> service.execute(new StartLoginCommand(MOBILE)))
                .isInstanceOfSatisfying(LoginApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LoginFailureReason.ACCOUNT_NOT_ACTIVE));
        verify(generateOtp, never()).generate(any());
    }

    private User activeUser() {
        return User.rehydrate(
                USER_ID, new Email("asha@example.com"), MOBILE,
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)), UserStatus.ACTIVE, java.util.Set.of(),
                AuditInfo.createdBy(AggregateId.newId(), NOW), 0);
    }

    private User pendingUser() {
        return User.rehydrate(
                USER_ID, new Email("asha@example.com"), MOBILE,
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)), UserStatus.PENDING_VERIFICATION,
                java.util.Set.of(), AuditInfo.createdBy(AggregateId.newId(), NOW), 0);
    }

    private OtpActionResult otpResult() {
        OtpVerification verification = OtpVerification.rehydrate(
                AggregateId.newId(), USER_ID, OtpHash.encoded("A".repeat(64)), OtpPurpose.SIGN_IN,
                NOW, NOW.plusSeconds(300), OtpStatus.PENDING, 0, 0,
                AuditInfo.createdBy(AggregateId.newId(), NOW), 0);
        return OtpActionResult.from(verification, List.of());
    }
}
