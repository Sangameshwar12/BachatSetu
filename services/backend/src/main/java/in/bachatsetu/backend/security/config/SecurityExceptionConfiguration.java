package in.bachatsetu.backend.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.security.exception.ProblemDetailsAccessDeniedHandler;
import in.bachatsetu.backend.security.exception.ProblemDetailsAuthenticationEntryPoint;
import in.bachatsetu.backend.security.exception.SecurityProblemWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Composes RFC 7807 security exception handlers. */
@Configuration(proxyBeanMethods = false)
public class SecurityExceptionConfiguration {

    @Bean
    SecurityProblemWriter securityProblemWriter(
            ObjectMapper objectMapper,
            @Qualifier("securityClock") Clock securityClock) {
        return new SecurityProblemWriter(objectMapper, securityClock);
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(SecurityProblemWriter writer) {
        return new ProblemDetailsAuthenticationEntryPoint(writer);
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(SecurityProblemWriter writer) {
        return new ProblemDetailsAccessDeniedHandler(writer);
    }
}
