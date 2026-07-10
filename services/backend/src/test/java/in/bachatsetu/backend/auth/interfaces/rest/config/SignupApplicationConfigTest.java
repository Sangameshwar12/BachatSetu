package in.bachatsetu.backend.auth.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.application.signup.service.CompleteSignupApplicationService;
import in.bachatsetu.backend.auth.application.signup.service.StartSignupApplicationService;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import org.junit.jupiter.api.Test;

class SignupApplicationConfigTest {

    private final SignupApplicationConfig config = new SignupApplicationConfig();

    @Test
    void composesStartSignupUseCase() {
        assertThat(config.startSignupUseCase(
                        mock(ProfileProvisioningPort.class), mock(UserRepository.class), mock(GenerateOtpUseCase.class),
                        mock(PasswordHashGeneratorPort.class), mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class)))
                .isInstanceOf(StartSignupApplicationService.class);
    }

    @Test
    void composesCompleteSignupUseCase() {
        assertThat(config.completeSignupUseCase(
                        mock(VerifyOtpUseCase.class), mock(UserRepository.class), mock(ProfileProvisioningPort.class),
                        mock(RoleRepository.class), mock(GenerateAccessTokenUseCase.class),
                        mock(GenerateRefreshTokenUseCase.class), mock(TenantProvider.class),
                        mock(DomainEventPublisherPort.class), mock(ClockPort.class)))
                .isInstanceOf(CompleteSignupApplicationService.class);
    }
}
