package in.bachatsetu.backend.auth.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.service.GenerateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.InvalidateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.ResendOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.VerifyOtpApplicationService;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationApplicationConfig;
import in.bachatsetu.backend.auth.interfaces.rest.config.AuthenticationOpenApiConfig;
import in.bachatsetu.backend.auth.interfaces.rest.exception.OtpRestException;
import io.swagger.v3.oas.models.OpenAPI;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthenticationRestConfigurationTest {

    @Test
    void composesEveryOtpUseCase() {
        AuthenticationApplicationConfig config = new AuthenticationApplicationConfig();
        UserRepository users = mock(UserRepository.class);
        OtpVerificationRepository verifications = mock(OtpVerificationRepository.class);
        ClockPort clock = mock(ClockPort.class);
        RandomGeneratorPort random = mock(RandomGeneratorPort.class);
        HashingPort hashing = mock(HashingPort.class);
        OtpSenderPort sender = mock(OtpSenderPort.class);
        OtpRequestValidator validator = config.otpRequestValidator(users);
        OtpPolicyService policy = config.otpPolicyService();

        assertThat(config.generateOtpUseCase(validator, verifications, policy, clock, random, hashing, sender))
                .isInstanceOf(GenerateOtpApplicationService.class);
        assertThat(config.verifyOtpUseCase(validator, verifications, clock, hashing))
                .isInstanceOf(VerifyOtpApplicationService.class);
        assertThat(config.resendOtpUseCase(validator, verifications, policy, clock, random, hashing, sender))
                .isInstanceOf(ResendOtpApplicationService.class);
        assertThat(config.invalidateOtpUseCase(validator, verifications, clock))
                .isInstanceOf(InvalidateOtpApplicationService.class);
    }

    @Test
    void definesOpenApiMetadata() {
        OpenAPI openApi = new AuthenticationOpenApiConfig().bachatSetuOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("BachatSetu API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getComponents()).isNotNull();
    }

    @Test
    void exposesRestExceptionMetadata() {
        URI type = URI.create("urn:bachatsetu:problem:otp-invalid");
        OtpRestException exception = new OtpRestException(
                HttpStatus.UNPROCESSABLE_ENTITY, type, "otp-invalid", "invalid");

        assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(exception.type()).isEqualTo(type);
        assertThat(exception.code()).isEqualTo("otp-invalid");
        assertThat(exception).hasMessage("invalid");
    }
}
