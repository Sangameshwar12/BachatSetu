package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.BCryptTokenHasherAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.JwtProviderAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SystemTokenClockAdapter;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.authentication.token",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(AuthenticationTokenProperties.class)
public class AuthenticationTokenInfrastructureConfig {

    @Bean
    TokenClockPort tokenClockPort(Clock authenticationClock) {
        return new SystemTokenClockAdapter(authenticationClock);
    }

    @Bean
    BCryptPasswordEncoder refreshTokenPasswordEncoder(
            AuthenticationTokenProperties properties,
            SecureRandom authenticationSecureRandom) {
        return new BCryptPasswordEncoder(
                BCryptVersion.$2A,
                properties.hashStrength(),
                authenticationSecureRandom);
    }

    @Bean
    TokenHasherPort tokenHasherPort(
            SecureRandom authenticationSecureRandom,
            BCryptPasswordEncoder refreshTokenPasswordEncoder) {
        return new BCryptTokenHasherAdapter(authenticationSecureRandom, refreshTokenPasswordEncoder);
    }

    @Bean
    JwtProviderPort jwtProviderPort(
            AuthenticationTokenProperties properties,
            TokenClockPort tokenClockPort) {
        return new JwtProviderAdapter(properties, tokenClockPort);
    }
}
