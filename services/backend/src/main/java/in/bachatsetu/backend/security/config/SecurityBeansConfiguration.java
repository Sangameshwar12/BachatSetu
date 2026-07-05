package in.bachatsetu.backend.security.config;

import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.security.context.CurrentUserService;
import in.bachatsetu.backend.security.filter.JwtAuthenticationFilter;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
