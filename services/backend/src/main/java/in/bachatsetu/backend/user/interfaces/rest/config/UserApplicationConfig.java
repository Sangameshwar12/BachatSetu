package in.bachatsetu.backend.user.interfaces.rest.config;

import in.bachatsetu.backend.user.application.port.ClockPort;
import in.bachatsetu.backend.user.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.user.application.port.TransactionPort;
import in.bachatsetu.backend.user.application.service.CompleteOnboardingApplicationService;
import in.bachatsetu.backend.user.application.usecase.CompleteOnboardingUseCase;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the framework-free user application services.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * {@code @ConditionalOnBean} check on {@code UserInfrastructureConfig}'s beans, for the same
 * non-deterministic component-scan ordering reason documented on
 * {@code NotificationApplicationConfig}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UserApplicationConfig {

    @Bean
    public CompleteOnboardingUseCase completeOnboardingUseCase(
            UserRepository userRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        return new CompleteOnboardingApplicationService(userRepository, eventPublisher, clock, transaction);
    }
}
