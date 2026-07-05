package in.bachatsetu.backend.auth.interfaces.rest.config;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.service.GenerateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.InvalidateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.ResendOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.VerifyOtpApplicationService;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free OTP application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    UserRepository.class,
    OtpVerificationRepository.class,
    ClockPort.class,
    RandomGeneratorPort.class,
    HashingPort.class,
    OtpSenderPort.class
})
public class AuthenticationApplicationConfig {

    @Bean
    public OtpRequestValidator otpRequestValidator(UserRepository userRepository) {
        return new OtpRequestValidator(userRepository);
    }

    @Bean
    public OtpPolicyService otpPolicyService() {
        return new OtpPolicyService();
    }

    @Bean
    public GenerateOtpUseCase generateOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender) {
        return new GenerateOtpApplicationService(
                validator, repository, policyService, clock, randomGenerator, hashing, sender);
    }

    @Bean
    public VerifyOtpUseCase verifyOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock,
            HashingPort hashing) {
        return new VerifyOtpApplicationService(validator, repository, clock, hashing);
    }

    @Bean
    public ResendOtpUseCase resendOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender) {
        return new ResendOtpApplicationService(
                validator, repository, policyService, clock, randomGenerator, hashing, sender);
    }

    @Bean
    public InvalidateOtpUseCase invalidateOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock) {
        return new InvalidateOtpApplicationService(validator, repository, clock);
    }
}
