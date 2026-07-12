package in.bachatsetu.backend.security.config;

import in.bachatsetu.backend.admin.application.configuration.service.FeatureFlagQueryService;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatusQueryService;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.security.context.CurrentUserService;
import in.bachatsetu.backend.security.context.SecurityContextCurrentAuditorProvider;
import in.bachatsetu.backend.security.filter.FeatureFlagEnforcementFilter;
import in.bachatsetu.backend.security.filter.JwtAuthenticationFilter;
import in.bachatsetu.backend.security.filter.MaintenanceModeFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Shared Spring Security beans with constructor-visible dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationSecurityProperties.class)
public class SecurityBeansConfiguration {

    @Bean("securityClock")
    Clock securityClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder(AuthenticationSecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.passwordHashStrength());
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CurrentUserProvider currentUserProvider() {
        return new CurrentUserService();
    }

    // Named distinctly from JpaAuditingConfig#currentAuditorProvider (its @ConditionalOnMissingBean
    // fallback), and marked @Primary rather than relying on that condition alone: both are plain
    // @Configuration classes with no guaranteed processing order relative to each other, so which
    // one @ConditionalOnMissingBean sees as "already present" is not deterministic — a same-named
    // @Bean method here would risk a BeanDefinitionOverrideException depending on classpath order.
    @Bean
    @Primary
    CurrentAuditorProvider securityContextCurrentAuditorProvider(
            @Value("${bachatsetu.persistence.auditing.system-actor-id}") UUID systemActorId) {
        return new SecurityContextCurrentAuditorProvider(systemActorId);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            ObjectProvider<ValidateAccessTokenUseCase> validatorProvider,
            AuthenticationSecurityProperties properties,
            AuthenticationEntryPoint authenticationEntryPoint) {
        return new JwtAuthenticationFilter(
                validatorProvider::getIfAvailable,
                properties,
                authenticationEntryPoint);
    }

    @Bean
    MaintenanceModeFilter maintenanceModeFilter(
            ObjectProvider<MaintenanceStatusQueryService> statusServiceProvider,
            @Qualifier("securityClock") Clock clock,
            ObjectMapper objectMapper) {
        return new MaintenanceModeFilter(statusServiceProvider::getIfAvailable, clock, objectMapper);
    }

    @Bean
    FeatureFlagEnforcementFilter featureFlagEnforcementFilter(
            ObjectProvider<FeatureFlagQueryService> featureFlagServiceProvider, ObjectMapper objectMapper) {
        return new FeatureFlagEnforcementFilter(featureFlagServiceProvider::getIfAvailable, objectMapper);
    }
}
