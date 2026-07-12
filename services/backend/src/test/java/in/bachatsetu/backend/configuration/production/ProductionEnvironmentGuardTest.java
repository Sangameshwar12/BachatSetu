package in.bachatsetu.backend.configuration.production;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentGuardTest {

    private static final String SIGNING_SECRET_KEY = "bachatsetu.authentication.token.signing-secret";
    private static final String DATABASE_PASSWORD_KEY = "spring.datasource.password";
    private static final String CORS_ORIGINS_KEY = "bachatsetu.authentication.security.cors.allowed-origins";
    private static final String TENANT_ID_KEY = "bachatsetu.tenancy.default-tenant-id";
    private static final String SYSTEM_ACTOR_ID_KEY = "bachatsetu.persistence.auditing.system-actor-id";
    private static final String LOCAL_DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";
    private static final String LOCAL_SYSTEM_ACTOR_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void startsWhenEveryProductionSecretIsProperlyConfigured() {
        MockEnvironment environment = validEnvironment();

        assertThatNoException().isThrownBy(() -> new ProductionEnvironmentGuard(environment));
    }

    @Test
    void refusesToStartWithoutAJwtSigningSecret() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(SIGNING_SECRET_KEY, "");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_JWT_SIGNING_SECRET");
    }

    @Test
    void refusesToStartWithTheDevelopmentDatabasePassword() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(DATABASE_PASSWORD_KEY, "bachatsetu");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_PASSWORD");
    }

    @Test
    void refusesToStartWithoutADatabasePassword() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(DATABASE_PASSWORD_KEY, "");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_PASSWORD");
    }

    @Test
    void refusesToStartWithTheLocalhostCorsDefault() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(CORS_ORIGINS_KEY, "http://localhost:3000");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_CORS_ALLOWED_ORIGINS");
    }

    @Test
    void refusesToStartWithoutAnyCorsOriginConfigured() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(CORS_ORIGINS_KEY, "");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_CORS_ALLOWED_ORIGINS");
    }

    @Test
    void refusesToStartWithoutATenantId() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(TENANT_ID_KEY, "");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TENANT_DEFAULT_ID");
    }

    @Test
    void refusesToStartWithTheLocalPlaceholderTenantId() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(TENANT_ID_KEY, LOCAL_DEFAULT_TENANT_ID);

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TENANT_DEFAULT_ID");
    }

    @Test
    void refusesToStartWithoutASystemActorId() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(SYSTEM_ACTOR_ID_KEY, "");

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUDIT_SYSTEM_ACTOR_ID");
    }

    @Test
    void refusesToStartWithTheLocalPlaceholderSystemActorId() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty(SYSTEM_ACTOR_ID_KEY, LOCAL_SYSTEM_ACTOR_ID);

        assertThatThrownBy(() -> new ProductionEnvironmentGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUDIT_SYSTEM_ACTOR_ID");
    }

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(SIGNING_SECRET_KEY, "a-sufficiently-long-production-signing-secret");
        environment.setProperty(DATABASE_PASSWORD_KEY, "a-real-production-password");
        environment.setProperty(CORS_ORIGINS_KEY, "https://app.bachatsetu.in");
        environment.setProperty(TENANT_ID_KEY, "8d6e6b8e-6b8e-4b8e-8b8e-6b8e6b8e6b8e");
        environment.setProperty(SYSTEM_ACTOR_ID_KEY, "7c5d5a7d-5a7d-3a7d-7a7d-5a7d5a7d5a7d");
        return environment;
    }
}
