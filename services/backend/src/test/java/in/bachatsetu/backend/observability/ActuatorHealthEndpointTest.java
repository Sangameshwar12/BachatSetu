package in.bachatsetu.backend.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the Kubernetes/ECS-style liveness and readiness probes are reachable without
 * authentication — the plain {@code /actuator/health} case is already covered by
 * {@link in.bachatsetu.backend.HealthEndpointTest}. This complements it: before this
 * sprint, {@code bachatsetu.authentication.security.public-endpoints} listed only the exact
 * path {@code /actuator/health}, so the sub-paths this test exercises would have returned
 * 401 rather than 200 — which would have made services/backend/Dockerfile's
 * {@code HEALTHCHECK} (and any container orchestrator's liveness probe) fail permanently.
 * {@code public-endpoints} now also includes {@code /actuator/health/**}.
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
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessProbeIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessProbeIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
