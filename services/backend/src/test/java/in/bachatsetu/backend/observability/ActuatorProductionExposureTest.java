package in.bachatsetu.backend.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the production Actuator posture introduced in Sprint 9.1: with
 * {@code management.endpoints.web.exposure.include} restricted to {@code health,info,prometheus}
 * (as {@code application-prod.yml} now sets it) and the matching
 * {@code bachatsetu.authentication.security.public-endpoints} allowlist (also as
 * {@code application-prod.yml} now sets it), health/info/prometheus are reachable without
 * authentication and every other endpoint — including the generic {@code metrics} endpoint,
 * which stays enabled in every other profile — is gone. This mirrors the properties directly
 * rather than activating the {@code prod} Spring profile itself, since that profile's
 * {@code ProductionEnvironmentGuard} deliberately fails startup without real database/JWT/CORS
 * secrets that a unit test has no reason to fabricate.
 */
@SpringBootTest(properties = {
    "bachatsetu.persistence.auditing.enabled=false",
    "bachatsetu.persistence.repositories.enabled=false",
    "bachatsetu.authentication.rest.enabled=false",
    "bachatsetu.authentication.token.enabled=false",
    "bachatsetu.group.rest.enabled=false",
    "bachatsetu.member.rest.enabled=false",
    "bachatsetu.user.rest.enabled=false",
    "bachatsetu.invitation.rest.enabled=false",
    "bachatsetu.dashboard.rest.enabled=false",
    "bachatsetu.payment.rest.enabled=false",
    "bachatsetu.draw.rest.enabled=false",
    "bachatsetu.receipt.rest.enabled=false",
    "bachatsetu.notification.rest.enabled=false",
    "bachatsetu.auction.rest.enabled=false",
    "bachatsetu.automation.enabled=false",
    "bachatsetu.payment.gateway.enabled=false",
    "bachatsetu.storage.enabled=false",
    "bachatsetu.audit.rest.enabled=false",
    "bachatsetu.admin.enabled=false",
    "bachatsetu.admin.analytics.enabled=false",
    "bachatsetu.admin.platform-config.enabled=false",
    "bachatsetu.support.rest.enabled=false",
    "bachatsetu.platform-operations.rest.enabled=false",
    "bachatsetu.cache.enabled=false",
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "management.endpoints.web.exposure.include=health,info,prometheus",
    "bachatsetu.authentication.security.public-endpoints[0]=/actuator/health",
    "bachatsetu.authentication.security.public-endpoints[1]=/actuator/health/**",
    "bachatsetu.authentication.security.public-endpoints[2]=/actuator/info",
    "bachatsetu.authentication.security.public-endpoints[3]=/actuator/prometheus",
    "bachatsetu.authentication.security.public-endpoints[4]=/api/v1/auth/**",
    "bachatsetu.authentication.security.public-endpoints[5]=/api/v1/payments/webhooks/**",
    "bachatsetu.authentication.security.public-endpoints[6]=/api/v1/join/**"
})
@AutoConfigureMockMvc
class ActuatorProductionExposureTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void infoIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    void prometheusIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void metricsEndpointIsUnreachable() throws Exception {
        // Rejected by Spring Security before Actuator's own dispatcher is even reached — the
        // public-endpoints allowlist no longer permits it, on top of Actuator itself no longer
        // exposing it. Either layer alone would already disable the endpoint; both do, together.
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }
}
