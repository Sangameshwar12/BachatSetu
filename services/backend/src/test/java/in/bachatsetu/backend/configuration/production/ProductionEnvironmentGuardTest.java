package in.bachatsetu.backend.configuration.production;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentGuardTest {

    private static final String SIGNING_SECRET_KEY = "bachatsetu.authentication.token.signing-secret";
    private static final String DATABASE_PASSWORD_KEY = "spring.datasource.password";
    private static final String CORS_ORIGINS_KEY = "bachatsetu.authentication.security.cors.allowed-origins";

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

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(SIGNING_SECRET_KEY, "a-sufficiently-long-production-signing-secret");
        environment.setProperty(DATABASE_PASSWORD_KEY, "a-real-production-password");
        environment.setProperty(CORS_ORIGINS_KEY, "https://app.bachatsetu.in");
        return environment;
    }
}
