package in.bachatsetu.backend.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final SecurityProblemWriter writer = new SecurityProblemWriter(
            objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void authenticationEntryPointWritesUnauthorizedProblemDetails() throws Exception {
        var entryPoint = new ProblemDetailsAuthenticationEntryPoint(writer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("hidden"));

        JsonNode problem = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(problem.get("type").asText()).isEqualTo("urn:bachatsetu:problem:authentication-required");
        assertThat(problem.get("title").asText()).isEqualTo("Authentication required");
        assertThat(problem.get("instance").asText()).isEqualTo("/protected");
        assertThat(problem.get("properties").get("code").asText()).isEqualTo("authentication-required");
        assertThat(problem.get("properties").get("timestamp").asText()).isEqualTo(NOW.toString());
        assertThat(response.getContentAsString()).doesNotContain("hidden");
    }

    @Test
    void accessDeniedHandlerWritesForbiddenProblemDetails() throws Exception {
        var handler = new ProblemDetailsAccessDeniedHandler(writer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("hidden"));

        JsonNode problem = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(problem.get("type").asText()).isEqualTo("urn:bachatsetu:problem:access-denied");
        assertThat(problem.get("title").asText()).isEqualTo("Access denied");
        assertThat(problem.get("detail").asText()).contains("not permitted");
        assertThat(response.getContentAsString()).doesNotContain("hidden");
    }
}
