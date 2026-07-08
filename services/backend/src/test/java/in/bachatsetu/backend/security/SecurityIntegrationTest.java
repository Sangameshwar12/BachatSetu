package in.bachatsetu.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationException;
import in.bachatsetu.backend.auth.application.token.port.JwtValidationFailure;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
    "bachatsetu.persistence.auditing.enabled=false",
    "bachatsetu.persistence.repositories.enabled=false",
    "bachatsetu.authentication.rest.enabled=false",
    "bachatsetu.group.rest.enabled=false",
    "bachatsetu.member.rest.enabled=false",
    "bachatsetu.payment.rest.enabled=false",
    "bachatsetu.draw.rest.enabled=false",
    "bachatsetu.receipt.rest.enabled=false",
    "bachatsetu.notification.rest.enabled=false",
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.ProbeConfiguration.class)
class SecurityIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer compact.jwt.value";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ValidateAccessTokenUseCase validateAccessToken;

    @Test
    void permitsConfiguredPublicAuthenticationEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/security-probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void rejectsProtectedEndpointWithoutTokenUsingProblemDetails() throws Exception {
        mockMvc.perform(get("/security-test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bachatsetu:problem:authentication-required"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void authenticatesProtectedRequestsWithoutCreatingSession() throws Exception {
        when(validateAccessToken.validate(any())).thenReturn(claims(Set.of("GROUP_MEMBER"), Set.of("group.read")));

        mockMvc.perform(get("/security-test/protected").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.tenantId").exists())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getRequest().getSession(false)).isNull());

        mockMvc.perform(post("/security-test/protected").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string("posted"));
    }

    @Test
    void rejectsInvalidBearerToken() throws Exception {
        when(validateAccessToken.validate(any())).thenThrow(new JwtValidationException(
                JwtValidationFailure.INVALID_SIGNATURE, "signature invalid"));

        mockMvc.perform(get("/security-test/protected").header(AUTHORIZATION, BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("signature invalid"))));
    }

    @Test
    void enforcesPreAuthorizeAndReturnsForbiddenProblemDetails() throws Exception {
        when(validateAccessToken.validate(any())).thenReturn(claims(Set.of("GROUP_MEMBER"), Set.of("group.read")));

        mockMvc.perform(get("/security-test/admin").header(AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:bachatsetu:problem:access-denied"))
                .andExpect(jsonPath("$.status").value(403));

        when(validateAccessToken.validate(any())).thenReturn(claims(Set.of("ADMIN"), Set.of()));
        mockMvc.perform(get("/security-test/admin").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void enforcesPostAuthorize() throws Exception {
        when(validateAccessToken.validate(any())).thenReturn(claims(Set.of("GROUP_MEMBER"), Set.of()));

        mockMvc.perform(get("/security-test/post-authorized").header(AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        when(validateAccessToken.validate(any())).thenReturn(claims(Set.of("GROUP_MEMBER"), Set.of("group.read")));
        mockMvc.perform(get("/security-test/post-authorized").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string("post-authorized"));
    }

    @Test
    void appliesConfiguredCorsPolicy() throws Exception {
        mockMvc.perform(options("/security-test/protected")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")));
    }

    private AccessTokenClaims claims(Set<String> roles, Set<String> permissions) {
        Instant issuedAt = Instant.parse("2026-07-06T00:00:00Z");
        return new AccessTokenClaims(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                roles,
                permissions,
                issuedAt,
                issuedAt.plusSeconds(900),
                "bachatsetu",
                "bachatsetu-api",
                1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        ProbeAuthorizationService probeAuthorizationService() {
            return new ProbeAuthorizationService();
        }

        @Bean
        SecurityProbeController securityProbeController(
                CurrentUserProvider currentUserProvider,
                ProbeAuthorizationService authorizationService) {
            return new SecurityProbeController(currentUserProvider, authorizationService);
        }
    }

    static class ProbeAuthorizationService {

        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "admin";
        }

        @PostAuthorize("hasAuthority('group.read')")
        String postAuthorized() {
            return "post-authorized";
        }
    }

    @RestController
    static final class SecurityProbeController {

        private final CurrentUserProvider currentUserProvider;
        private final ProbeAuthorizationService authorizationService;

        SecurityProbeController(
                CurrentUserProvider currentUserProvider,
                ProbeAuthorizationService authorizationService) {
            this.currentUserProvider = currentUserProvider;
            this.authorizationService = authorizationService;
        }

        @GetMapping("/api/v1/auth/security-probe")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/security-test/protected")
        AuthenticatedIdentity protectedEndpoint() {
            var user = currentUserProvider.requireCurrentUser();
            return new AuthenticatedIdentity(user.userId().toString(), user.tenantId().toString());
        }

        @PostMapping("/security-test/protected")
        String protectedPost() {
            currentUserProvider.requireCurrentUser();
            return "posted";
        }

        @GetMapping("/security-test/admin")
        String admin() {
            return authorizationService.admin();
        }

        @GetMapping("/security-test/post-authorized")
        String postAuthorized() {
            return authorizationService.postAuthorized();
        }
    }

    record AuthenticatedIdentity(String userId, String tenantId) {
    }
}
