package in.bachatsetu.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.security.exception.ProblemDetailsAccessDeniedHandler;
import in.bachatsetu.backend.security.exception.ProblemDetailsAuthenticationEntryPoint;
import in.bachatsetu.backend.security.filter.JwtAuthenticationFilter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class AuthenticationSecurityConfigurationTest {

    @Test
    void createsSecurityBeansAndCorsConfiguration() throws Exception {
        AuthenticationSecurityProperties properties = properties();
        SecurityBeansConfiguration beans = new SecurityBeansConfiguration();
        SecurityExceptionConfiguration exceptions = new SecurityExceptionConfiguration();
        SecurityConfiguration security = new SecurityConfiguration();
        Clock clock = beans.securityClock();
        var writer = exceptions.securityProblemWriter(new ObjectMapper().findAndRegisterModules(), clock);
        AuthenticationEntryPoint entryPoint = exceptions.authenticationEntryPoint(writer);
        ObjectProvider<ValidateAccessTokenUseCase> provider = mock(ObjectProvider.class);
        ValidateAccessTokenUseCase validator = mock(ValidateAccessTokenUseCase.class);
        when(provider.getIfAvailable()).thenReturn(validator);
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        PasswordEncoder passwordEncoder = beans.passwordEncoder(properties);
        CurrentUserProvider currentUser = beans.currentUserProvider();
        JwtAuthenticationFilter filter = beans.jwtAuthenticationFilter(provider, properties, entryPoint);
        CorsConfiguration cors = security.corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/resource"));

        assertThat(clock).isNotNull();
        assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
        assertThat(beans.authenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
        assertThat(currentUser.currentUser()).isEmpty();
        assertThat(filter).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("https://app.example.com");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(cors.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(cors.getExposedHeaders()).containsExactly("X-Request-ID");
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getMaxAge()).isEqualTo(Duration.ofHours(1).toSeconds());
        assertThat(exceptions.accessDeniedHandler(writer)).isInstanceOf(ProblemDetailsAccessDeniedHandler.class);
        assertThat(entryPoint).isInstanceOf(ProblemDetailsAuthenticationEntryPoint.class);
    }

    @Test
    void validatesSecurityAndCorsPropertyBoundaries() {
        assertThat(properties().clockSkew()).isEqualTo(Duration.ofSeconds(30));
        assertThatIllegalArgumentException().isThrownBy(() -> properties("Bad Header", "Bearer ",
                Duration.ZERO, 12, List.of("/health"), cors(false, List.of("https://app.example.com"), Duration.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties("Authorization", "Bearer",
                Duration.ZERO, 12, List.of("/health"), cors(false, List.of("https://app.example.com"), Duration.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties("Authorization", "Bearer ",
                Duration.ofMinutes(3), 12, List.of("/health"), cors(false, List.of("https://app.example.com"), Duration.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties("Authorization", "Bearer ",
                Duration.ZERO, 9, List.of("/health"), cors(false, List.of("https://app.example.com"), Duration.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties("Authorization", "Bearer ",
                Duration.ZERO, 12, List.of("relative"), cors(false, List.of("https://app.example.com"), Duration.ZERO)));
        assertThatIllegalArgumentException().isThrownBy(() -> cors(
                false, List.of("https://app.example.com"), Duration.ofSeconds(-1)));
        assertThatIllegalArgumentException().isThrownBy(() -> cors(true, List.of("*"), Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new AuthenticationSecurityProperties.Cors(
                List.of(""), List.of("GET"), List.of("Authorization"), List.of(), false, Duration.ZERO));
    }

    private AuthenticationSecurityProperties properties() {
        return properties(
                "Authorization",
                "Bearer ",
                Duration.ofSeconds(30),
                12,
                List.of("/actuator/health", "/api/v1/auth/**"),
                cors(false, List.of("https://app.example.com"), Duration.ofHours(1)));
    }

    private AuthenticationSecurityProperties properties(
            String header,
            String prefix,
            Duration skew,
            int strength,
            List<String> endpoints,
            AuthenticationSecurityProperties.Cors cors) {
        return new AuthenticationSecurityProperties(header, prefix, skew, strength, endpoints, cors);
    }

    private AuthenticationSecurityProperties.Cors cors(
            boolean credentials,
            List<String> origins,
            Duration maxAge) {
        return new AuthenticationSecurityProperties.Cors(
                origins,
                List.of("GET", "POST"),
                List.of("Authorization", "Content-Type"),
                List.of("X-Request-ID"),
                credentials,
                maxAge);
    }
}
