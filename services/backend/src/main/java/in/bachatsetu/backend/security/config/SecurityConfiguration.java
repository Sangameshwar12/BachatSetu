package in.bachatsetu.backend.security.config;

import in.bachatsetu.backend.security.filter.FeatureFlagEnforcementFilter;
import in.bachatsetu.backend.security.filter.JwtAuthenticationFilter;
import in.bachatsetu.backend.security.filter.MaintenanceModeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless HTTP and method-security policy for the backend. */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSecurityProperties properties,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            MaintenanceModeFilter maintenanceModeFilter,
            FeatureFlagEnforcementFilter featureFlagEnforcementFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        String[] publicEndpoints = properties.publicEndpoints().toArray(String[]::new);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(featureFlagEnforcementFilter, MaintenanceModeFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthenticationSecurityProperties properties) {
        AuthenticationSecurityProperties.Cors configured = properties.cors();
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(configured.allowedOrigins());
        cors.setAllowedMethods(configured.allowedMethods());
        cors.setAllowedHeaders(configured.allowedHeaders());
        cors.setExposedHeaders(configured.exposedHeaders());
        cors.setAllowCredentials(configured.allowCredentials());
        cors.setMaxAge(configured.maxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
