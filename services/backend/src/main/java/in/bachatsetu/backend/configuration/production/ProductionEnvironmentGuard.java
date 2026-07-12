package in.bachatsetu.backend.configuration.production;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup fast, under the {@code prod} profile only, if a secret or
 * setting still holds a value that is only ever safe for local development — rather than
 * discovering the gap later as a mysterious authentication failure or an open CORS policy
 * in production. Every check here mirrors a default defined in application.yml or
 * application-local.yml that {@code prod} must never silently inherit.
 */
@Component
@Profile("prod")
public final class ProductionEnvironmentGuard {

    private static final String LOCALHOST_CORS_ORIGIN = "http://localhost:3000";
    private static final Set<String> WEAK_DATABASE_PASSWORDS = Set.of("bachatsetu", "password", "postgres", "");
    private static final String LOCAL_DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";
    private static final String LOCAL_SYSTEM_ACTOR_ID = "00000000-0000-0000-0000-000000000001";

    public ProductionEnvironmentGuard(Environment environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        requireNonBlank(environment, "AUTH_JWT_SIGNING_SECRET", "bachatsetu.authentication.token.signing-secret");
        requireNotWeakDatabasePassword(environment);
        requireCorsOriginConfigured(environment);
        requireNotThePlaceholder(environment, "TENANT_DEFAULT_ID", "bachatsetu.tenancy.default-tenant-id",
                LOCAL_DEFAULT_TENANT_ID);
        requireNotThePlaceholder(environment, "AUDIT_SYSTEM_ACTOR_ID", "bachatsetu.persistence.auditing.system-actor-id",
                LOCAL_SYSTEM_ACTOR_ID);
    }

    private void requireNonBlank(Environment environment, String variableName, String propertyKey) {
        String value = environment.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Refusing to start under the 'prod' profile: " + variableName + " is not set.");
        }
    }

    private void requireNotWeakDatabasePassword(Environment environment) {
        String password = environment.getProperty("spring.datasource.password", "");
        if (WEAK_DATABASE_PASSWORDS.contains(password)) {
            throw new IllegalStateException(
                    "Refusing to start under the 'prod' profile: DATABASE_PASSWORD is unset or a known "
                            + "development placeholder value.");
        }
    }

    private void requireNotThePlaceholder(
            Environment environment, String variableName, String propertyKey, String placeholderValue) {
        String value = environment.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Refusing to start under the 'prod' profile: " + variableName + " is not set.");
        }
        if (value.equals(placeholderValue)) {
            throw new IllegalStateException(
                    "Refusing to start under the 'prod' profile: " + variableName + " is still the local "
                            + "development placeholder value.");
        }
    }

    private void requireCorsOriginConfigured(Environment environment) {
        String origins = environment.getProperty("bachatsetu.authentication.security.cors.allowed-origins", "");
        Set<String> distinctOrigins = Arrays.stream(origins.split(","))
                .map(String::strip)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toSet());
        if (distinctOrigins.isEmpty() || distinctOrigins.equals(Set.of(LOCALHOST_CORS_ORIGIN))) {
            throw new IllegalStateException(
                    "Refusing to start under the 'prod' profile: AUTH_CORS_ALLOWED_ORIGINS is unset or still "
                            + "the localhost development default.");
        }
    }
}
