package in.bachatsetu.backend.auth.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.auth.application.login.service.CompleteLoginApplicationService;
import in.bachatsetu.backend.auth.application.login.service.StartLoginApplicationService;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import org.junit.jupiter.api.Test;

class LoginApplicationConfigTest {

    private final LoginApplicationConfig config = new LoginApplicationConfig();

    @Test
    void composesStartLoginUseCase() {
        assertThat(config.startLoginUseCase(mock(UserRepository.class), mock(GenerateOtpUseCase.class)))
                .isInstanceOf(StartLoginApplicationService.class);
    }

    @Test
    void composesCompleteLoginUseCase() {
        assertThat(config.completeLoginUseCase(
                        mock(VerifyOtpUseCase.class), mock(UserRepository.class),
                        mock(GenerateAccessTokenUseCase.class), mock(GenerateRefreshTokenUseCase.class),
                        mock(TenantProvider.class)))
                .isInstanceOf(CompleteLoginApplicationService.class);
    }
}
